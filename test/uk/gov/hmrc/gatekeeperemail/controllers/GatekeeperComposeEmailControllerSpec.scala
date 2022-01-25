/*
 * Copyright 2022 HM Revenue & Customs
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

import akka.stream.Materializer
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.http.Fault
import com.mongodb.client.result.InsertOneResult
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.mongodb.scala.bson.BsonNumber
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.PlayBodyParsers
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.gatekeeperemail.connectors.{GatekeeperEmailConnector, GatekeeperEmailRendererConnector}
import uk.gov.hmrc.gatekeeperemail.models.{Email, EmailData, EmailRequest, EmailSaved, EmailTemplateData, RenderResult}
import uk.gov.hmrc.gatekeeperemail.repositories.EmailRepository
import uk.gov.hmrc.gatekeeperemail.services.EmailService
import uk.gov.hmrc.http.HeaderCarrier

import java.io.IOException
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class GatekeeperComposeEmailControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar with ArgumentMatchersSugar {
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"     -> false,
        "metrics.enabled" -> false
      )
      .build()

  val emailId = "email@example.com"
  val subject = "Email subject"
  val emailBody = "Body to be used in the email template"
  val emailServicePath = "/gatekeeper/email"
  val templateData = EmailTemplateData("templateId", Map(), false, Map(), None)
  val email = Email("emailId-123", templateData, "DL Team", List("test@digital.hmrc.gov.uk"), None, "markdownEmailBody", "This is test email",
    "test subject", "composedBy", Some("approvedBy"), DateTime.now(UTC))

  val emailRequest = EmailRequest(List(emailId), "gatekeeper", EmailData(emailId, subject, emailBody))
  val wrongEmailRequest = EmailRequest(List(emailId), "gatekeeper", EmailData(emailId, subject, emailBody))
  private val fakeRequest = FakeRequest("POST", "/gatekeeper-email").withBody(Json.toJson(emailRequest))
  private val fakeSaveEmailRequest = FakeRequest("POST", "/gatekeeper-email/save-email").withBody(Json.toJson(emailRequest))
  private val fakeWrongSaveEmailRequest = FakeRequest("POST", "/gatekeeper-email/save-email").withBody(Json.toJson(emailBody))
  lazy implicit val mat: Materializer = app.materializer
  private val playBodyParsers: PlayBodyParsers = app.injector.instanceOf[PlayBodyParsers]
  val mockEmailConnector: GatekeeperEmailConnector = mock[GatekeeperEmailConnector]
  val mockEmailRepository: EmailRepository = mock[EmailRepository]
  val emailRendererConnectorMock: GatekeeperEmailRendererConnector = mock[GatekeeperEmailRendererConnector]

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure("metrics.enabled" -> false, "auditing.enabled" -> false)
    .build()

  trait FailingHttp {
    self: Setup =>
    stubFor(post(urlEqualTo(emailServicePath)).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)))
  }

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val emailService = new EmailService(mockEmailConnector, emailRendererConnectorMock, mockEmailRepository)
    val mockEmailService = mock[EmailService]
    val controller = new GatekeeperComposeEmailController(Helpers.stubMessagesControllerComponents(),
      playBodyParsers, emailService)
    val controller2 = new GatekeeperComposeEmailController(Helpers.stubMessagesControllerComponents(),
      playBodyParsers, mockEmailService)

    when(emailRendererConnectorMock.getTemplatedEmail(*))
      .thenReturn(successful(Right(RenderResult("RGVhciB1c2VyLCBUaGlzIGlzIGEgdGVzdCBtYWls",
        "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA==", "from@digital.hmrc.gov.uk", "subject", ""))))

    val emailId: String = UUID.randomUUID().toString
    val dummyEmailData = Email("", EmailTemplateData("", Map(), false, Map(), None), "", List(),
      None, "", "", "", "", None, DateTime.now)
    when(mockEmailRepository.getEmailData(EmailSaved(emailId))).thenReturn(Future(dummyEmailData))
  }

  "POST /gatekeeper-email/send-email" should {
    "return 200" in new Setup {
      when(mockEmailConnector.sendEmail(*)).thenReturn(successful(Status.OK))
      when(mockEmailRepository.persist(*)).thenReturn(Future(InsertOneResult.acknowledged(BsonNumber(1))))
      val result = controller.sendEmail(emailId)(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 500" in new Setup {
      when(mockEmailConnector.sendEmail(*)).thenReturn(failed(new IOException("can not connect to email service")))
      when(mockEmailRepository.persist(*)).thenReturn(Future(InsertOneResult.acknowledged(BsonNumber(1))))
      val result = controller.sendEmail(emailId)(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "POST /gatekeeper-email/save-email" should {
    "return 200" in new Setup {
      when(mockEmailService.persistEmail(emailRequest)).thenReturn(successful(email))
      val result = controller2.saveEmail()(fakeSaveEmailRequest)
      status(result) shouldBe Status.OK
    }

    "return 500" in new Setup {
      when(mockEmailService.persistEmail(emailRequest)).thenReturn(failed(new IOException("can not connect to email service")))
      val result = controller2.saveEmail()(fakeSaveEmailRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return 400" in new Setup {
      when(mockEmailService.persistEmail(emailRequest)).thenReturn(successful(email))
      val result = controller2.saveEmail()(fakeWrongSaveEmailRequest)
      status(result) shouldBe Status.BAD_REQUEST
    }
  }
}
