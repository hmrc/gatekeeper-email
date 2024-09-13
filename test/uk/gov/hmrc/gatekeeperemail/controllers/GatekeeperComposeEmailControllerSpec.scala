/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.gatekeeperemail.controllers

import java.io.IOException
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

import com.mongodb.client.result.{InsertManyResult, InsertOneResult}
import org.apache.pekko.stream.Materializer
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.mongodb.scala.bson.BsonNumber
import org.scalatest.matchers.should.Matchers

import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.connectors.{ApmConnector, DeveloperConnector, EmailConnector, GatekeeperEmailRendererConnector}
import uk.gov.hmrc.gatekeeperemail.models.EmailStatus.SENT
import uk.gov.hmrc.gatekeeperemail.models._
import uk.gov.hmrc.gatekeeperemail.models.requests.{DevelopersEmailQuery, EmailData, EmailRequest, TestEmailRequest}
import uk.gov.hmrc.gatekeeperemail.repositories.{DraftEmailRepository, SentEmailRepository}
import uk.gov.hmrc.gatekeeperemail.services.DraftEmailService
import uk.gov.hmrc.gatekeeperemail.stride.connectors.AuthConnector
import uk.gov.hmrc.gatekeeperemail.stride.controllers.actions.ForbiddenHandler

class GatekeeperComposeEmailControllerSpec extends AbstractControllerSpec with Matchers with MockitoSugar with ArgumentMatchersSugar with FixedClock {

  private val subject      = "Email subject"
  private val emailBody    = "Body to be used in the email template"
  private val templateData = EmailTemplateData("templateId", Map(), false, Map(), None)
  val emailPreferences     = DevelopersEmailQuery()

  private val draftEmail = DraftEmail(
    "emailId-123",
    templateData,
    "DL Team",
    emailPreferences,
    "markdownEmailBody",
    "This is test email",
    "test subject",
    SENT,
    "composedBy",
    Some("approvedBy"),
    instant,
    1
  )

  private val emailRequest = EmailRequest(emailPreferences, "gatekeeper", EmailData(subject, emailBody))

  private val fakeSaveEmailRequest            = FakeRequest("POST", "/gatekeeper-email/save-email").withBody(Json.toJson(emailRequest))
  private val fakeDeleteEmailRequest          = FakeRequest("POST", "/gatekeeper-email/delete-email")
  private val fakeRequestWithBodyNotValidJson = FakeRequest("POST", "/gatekeeper-email/save-email").withBody(Json.toJson(emailBody))

  lazy implicit val mat: Materializer                                      = app.materializer
  private val requestConverter: RequestConverter                           = app.injector.instanceOf[RequestConverter]
  private val forbiddenHandler                                             = app.injector.instanceOf[ForbiddenHandler]
  private val mockEmailConnector: EmailConnector                           = mock[EmailConnector]
  private val mockDraftEmailRepository: DraftEmailRepository               = mock[DraftEmailRepository]
  private val mockSentEmailRepository: SentEmailRepository                 = mock[SentEmailRepository]
  private val mockEmailRendererConnector: GatekeeperEmailRendererConnector = mock[GatekeeperEmailRendererConnector]

  trait Setup extends AbstractSetup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val mockAppConfig                              = mock[AppConfig]
    val developerConnectorMock: DeveloperConnector = mock[DeveloperConnector]
    val apmConnectorMock: ApmConnector             = mock[ApmConnector]

    val emailService =
      new DraftEmailService(mockEmailRendererConnector, developerConnectorMock, apmConnectorMock, mockDraftEmailRepository, mockSentEmailRepository, mockAppConfig, clock)

    val mockEmailService = mock[DraftEmailService]

    val mockAuthConnector = mock[AuthConnector]

    val controller = new GatekeeperComposeEmailController(
      strideAuthConfig,
      AuthConnectorMock.aMock,
      forbiddenHandler,
      requestConverter,
      stubMessagesControllerComponents(),
      mockEmailService
    )

    when(mockEmailRendererConnector.getTemplatedEmail(*))
      .thenReturn(successful(Right(RenderResult(
        "RGVhciB1c2VyLCBUaGlzIGlzIGEgdGVzdCBtYWls",
        "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA==",
        "from@digital.hmrc.gov.uk",
        "subject",
        ""
      ))))

    val emailUUID: String = UUID.randomUUID().toString

    val emailAddress = "test@email.com"

    val fakeEmailRequest = FakeRequest("POST", s"/gatekeeper-email/send-test-email/$emailUUID")
      .withHeaders("Content-Type" -> "application/json")
      .withBody(Json.toJson(TestEmailRequest(emailAddress)))
    val dummyEmailData   = DraftEmail("", EmailTemplateData("", Map(), false, Map(), None), "", emailPreferences, "", "", "", SENT, "", None, instant, 1)
    when(mockDraftEmailRepository.getEmailData(emailUUID)).thenReturn(Future(dummyEmailData))
  }

  "POST /gatekeeper-email/send-email" should {
    "return 200" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      when(mockEmailService.sendEmail(emailUUID)).thenReturn(successful(draftEmail))
      when(mockEmailConnector.sendEmail(*)).thenReturn(successful(true))
      when(mockDraftEmailRepository.persist(*)).thenReturn(Future(InsertOneResult.acknowledged(BsonNumber(1))))
      when(mockDraftEmailRepository.updateEmailSentStatus(*, *)).thenReturn(successful(draftEmail))
      when(mockSentEmailRepository.persist(*)).thenReturn(Future(InsertManyResult.unacknowledged()))
      val result = controller.sendEmail(emailUUID)(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 500" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      when(mockEmailService.sendEmail(emailUUID)).thenReturn(failed(new IOException("mongo error")))
      val result = controller.sendEmail(emailUUID)(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return 403 for InsufficientEnrolments" in new Setup {
      AuthConnectorMock.Authorise.thenReturnInsufficientEnrolments()
      val result = controller.saveEmail(emailUUID)(fakeSaveEmailRequest)
      status(result) shouldBe Status.FORBIDDEN
    }

    "return 303 redirect to authorise when no session exists" in new Setup {
      AuthConnectorMock.Authorise.thenReturnSessionRecordNotFound()
      val result = controller.saveEmail(emailUUID)(fakeSaveEmailRequest)
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 403 for enrolments but no Name" in new Setup {
      AuthConnectorMock.Authorise.thenReturnNoName()
      val result = controller.saveEmail(emailUUID)(fakeSaveEmailRequest)
      status(result) shouldBe Status.FORBIDDEN
    }
  }

  "POST /gatekeeper-email/send-test-email" should {
    "return 200" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      when(mockEmailService.sendEmail(emailUUID, emailAddress)).thenReturn(successful(draftEmail))
      val result = controller.sendTestEmail(emailUUID)(fakeEmailRequest)
      status(result) shouldBe Status.OK
    }

    "return 500" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      when(mockEmailService.sendEmail(emailUUID, emailAddress)).thenReturn(failed(new IOException("mongo error")))
      val result = controller.sendTestEmail(emailUUID)(fakeEmailRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "POST /gatekeeper-email/save-email" should {
    "return 200 when email is saved successfully" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      when(mockEmailService.persistEmail(emailRequest, emailUUID)).thenReturn(successful(draftEmail))
      val result = controller.saveEmail(emailUUID)(fakeSaveEmailRequest)
      status(result) shouldBe Status.OK
    }

    "return 500" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      when(mockEmailService.persistEmail(emailRequest, emailUUID)).thenReturn(failed(new IOException("can not connect to email service")))
      val result = controller.saveEmail(emailUUID)(fakeSaveEmailRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return 400 when body is not JSON" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      val result = controller.saveEmail(emailUUID)(fakeRequestWithBodyNotValidJson)
      status(result) shouldBe Status.BAD_REQUEST
      verifyNoMoreInteractions(mockEmailService)
    }

    "return 403 for InsufficientEnrolments" in new Setup {
      AuthConnectorMock.Authorise.thenReturnInsufficientEnrolments()
      val result = controller.saveEmail(emailUUID)(fakeSaveEmailRequest)
      status(result) shouldBe Status.FORBIDDEN
    }

    "return 403 for enrolments but no Name" in new Setup {
      AuthConnectorMock.Authorise.thenReturnNoName()
      val result = controller.saveEmail(emailUUID)(fakeSaveEmailRequest)
      status(result) shouldBe Status.FORBIDDEN
    }

    "return 303 redirect to authorise when no session exists" in new Setup {
      AuthConnectorMock.Authorise.thenReturnSessionRecordNotFound()
      val result = controller.saveEmail(emailUUID)(fakeSaveEmailRequest)
      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "POST /gatekeeper-email/delete-email" should {
    "return 200" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      when(mockEmailService.deleteEmail(emailUUID)).thenReturn(successful(true))
      val result = controller.deleteEmail(emailUUID)(fakeDeleteEmailRequest)
      status(result) shouldBe Status.OK
    }

    "return 500" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      when(mockEmailService.deleteEmail(emailUUID)).thenReturn(failed(new IOException("can not connect to email service")))
      val result = controller.deleteEmail(emailUUID)(fakeDeleteEmailRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "POST /gatekeeper-email/update-email" should {
    "return 200" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      when(mockEmailService.updateEmail(*, *)).thenReturn(successful(draftEmail))
      val result = controller.updateEmail(emailUUID)(fakeSaveEmailRequest)
      status(result) shouldBe Status.OK
    }

    "return 500" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      when(mockEmailService.updateEmail(emailRequest, emailUUID)).thenReturn(failed(new IOException("can not connect to email service")))
      val result = controller.updateEmail(emailUUID)(fakeSaveEmailRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return 400" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      when(mockEmailService.updateEmail(emailRequest, emailUUID)).thenReturn(successful(draftEmail))
      val result = controller.updateEmail(emailUUID)(fakeRequestWithBodyNotValidJson)
      status(result) shouldBe Status.BAD_REQUEST
    }

    "return 403 when authentication has insufficient enrolmwents" in new Setup {
      AuthConnectorMock.Authorise.thenReturnInsufficientEnrolments()
      val result = controller.updateEmail(emailUUID)(fakeSaveEmailRequest)
      status(result) shouldBe Status.FORBIDDEN
    }

    "return 403 for enrolments but no Name" in new Setup {
      AuthConnectorMock.Authorise.thenReturnNoName()
      val result = controller.saveEmail(emailUUID)(fakeSaveEmailRequest)
      status(result) shouldBe Status.FORBIDDEN
    }

    "return 303 redirect to authorise when no session exists" in new Setup {
      AuthConnectorMock.Authorise.thenReturnSessionRecordNotFound()
      val result = controller.saveEmail(emailUUID)(fakeSaveEmailRequest)
      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "GET /gatekeeper-email/fetch-email" should {
    "return 200" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      when(mockEmailService.fetchEmail(emailUUID)).thenReturn(successful(draftEmail))
      val result = controller.fetchEmail(emailUUID)(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 403 when authentication has insufficient enrolments" in new Setup {
      AuthConnectorMock.Authorise.thenReturnInsufficientEnrolments()
      val result = controller.fetchEmail(emailUUID)(fakeRequest)
      status(result) shouldBe Status.FORBIDDEN
    }

    "return 303 redirect to authorise when no session exists" in new Setup {
      AuthConnectorMock.Authorise.thenReturnSessionRecordNotFound()
      val result = controller.fetchEmail(emailUUID)(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
    }
  }
}
