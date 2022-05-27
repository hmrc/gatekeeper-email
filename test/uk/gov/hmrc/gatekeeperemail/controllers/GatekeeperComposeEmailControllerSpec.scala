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

import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime.now
import java.util.UUID

import akka.stream.Materializer
import com.mongodb.client.result.{InsertManyResult, InsertOneResult}
import org.mockito.ArgumentMatchers.anyString
import org.mockito.{ArgumentCaptor, ArgumentMatchersSugar, MockitoSugar}
import org.mongodb.scala.bson.BsonNumber
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.PlayBodyParsers
import play.api.test.FakeRequest
import play.api.test.Helpers.{stubMessagesControllerComponents, _}
import uk.gov.hmrc.apiplatform.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.apiplatform.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.connectors.{GatekeeperEmailConnector, GatekeeperEmailRendererConnector}
import uk.gov.hmrc.gatekeeperemail.models.EmailStatus.SENT
import uk.gov.hmrc.gatekeeperemail.models._
import uk.gov.hmrc.gatekeeperemail.repositories.{DraftEmailRepository, SentEmailRepository}
import uk.gov.hmrc.gatekeeperemail.services.{DraftEmailService, ObjectStoreService}
import uk.gov.hmrc.gatekeeperemail.stride.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient
import uk.gov.hmrc.objectstore.client.{Md5Hash, ObjectSummaryWithMd5, Path}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class GatekeeperComposeEmailControllerSpec extends AbstractControllerSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar with ArgumentMatchersSugar {

  val emailId = "email@example.com"
  val subject = "Email subject"
  val emailBody = "Body to be used in the email template"
  val emailServicePath = "/gatekeeper/email"
  val templateData = EmailTemplateData("templateId", Map(), false, Map(), None)
  val users = List(User("example@example.com", "first name", "last name", true),
    User("example2@example2.com", "first name2", "last name2", true))
  private val draftEmail = DraftEmail("emailId-123", templateData, "DL Team",
    users, None, "markdownEmailBody", "This is test email",
    "test subject", SENT, "composedBy", Some("approvedBy"), now())
  val emailUUIDToAttachFile = "emailUUID111"
  val cargo = Some(UploadCargo(emailUUIDToAttachFile))
  val uploadedFile123: UploadedFileWithObjectStore = UploadedFileWithObjectStore("Ref123", "/gatekeeper/downloadUrl/123", "", "", "file123", "",
    1024, cargo, None, None, Some(s"/gatekeeper/$emailUUIDToAttachFile"), None)
  val uploadedFileSeq = Seq(uploadedFile123)
  val uploadedFileMetadata: UploadedFileMetadata = UploadedFileMetadata(Nonce.random, uploadedFileSeq, cargo)
  val emailRequest = EmailRequest(users, "gatekeeper", EmailData(subject, emailBody))
  val wrongEmailRequest = EmailRequest(users, "gatekeeper", EmailData(subject, emailBody))
  private val fakeRequestToUpdateFiles = FakeRequest("POST", "/gatekeeperemail/updatefiles")
    .withHeaders("Content-Type" -> "application/json")
    .withBody(Json.toJson(uploadedFileMetadata))
  private val fakeSaveEmailRequest = FakeRequest("POST", "/gatekeeper-email/save-email").withBody(Json.toJson(emailRequest))
  private val fakeDeleteEmailRequest = FakeRequest("POST", "/gatekeeper-email/delete-email")
  private val fakeRequestWithBodyNotValidJson = FakeRequest("POST", "/gatekeeper-email/save-email").withBody(Json.toJson(emailBody))
  lazy implicit val mat: Materializer = app.materializer
  private val playBodyParsers: PlayBodyParsers = app.injector.instanceOf[PlayBodyParsers]
  private val requestConverter: RequestConverter = app.injector.instanceOf[RequestConverter]
  val strideAuthConfig = app.injector.instanceOf[StrideAuthConfig]
  val forbiddenHandler = app.injector.instanceOf[ForbiddenHandler]

  val mockEmailConnector: GatekeeperEmailConnector = mock[GatekeeperEmailConnector]
  val mockDraftEmailRepository: DraftEmailRepository = mock[DraftEmailRepository]
  val mockSentEmailRepository: SentEmailRepository = mock[SentEmailRepository]
  val mockEmailRendererConnector: GatekeeperEmailRendererConnector = mock[GatekeeperEmailRendererConnector]

  trait Setup extends AbstractSetup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val objectStoreClient = mock[PlayObjectStoreClient]
    val mockAppConfig = mock[AppConfig]

    val emailService = new DraftEmailService(mockEmailRendererConnector, mockDraftEmailRepository, mockSentEmailRepository)
    val mockEmailService = mock[DraftEmailService]
    val mockObjectStoreService = mock[ObjectStoreService]
    val mockAuthConnector = mock[AuthConnector]
    val controller = new GatekeeperComposeEmailController(strideAuthConfig, AuthConnectorMock.aMock, forbiddenHandler,
      requestConverter, stubMessagesControllerComponents(), playBodyParsers, mockEmailService, mockObjectStoreService)
    val controller2 = new GatekeeperComposeEmailController(strideAuthConfig, AuthConnectorMock.aMock, forbiddenHandler,
      requestConverter, stubMessagesControllerComponents(), playBodyParsers, mockEmailService, mockObjectStoreService)

    when(mockEmailRendererConnector.getTemplatedEmail(*))
      .thenReturn(successful(Right(RenderResult("RGVhciB1c2VyLCBUaGlzIGlzIGEgdGVzdCBtYWls",
        "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA==", "from@digital.hmrc.gov.uk", "subject", ""))))

    val emailUUID: String = UUID.randomUUID().toString
    val dummyEmailData = DraftEmail("", EmailTemplateData("", Map(), false, Map(), None), "", List(),
      None, "", "", "", SENT, "", None, now)
    when(mockDraftEmailRepository.getEmailData(emailUUID)).thenReturn(Future(dummyEmailData))
  }

  "POST /gatekeeper-email/send-email" should {
    "return 200" in new Setup {
      when(mockEmailService.sendEmail(emailUUID)).thenReturn(successful(draftEmail))
      when(mockEmailConnector.sendEmail(*)).thenReturn(successful(Status.OK))
      when(mockDraftEmailRepository.persist(*)).thenReturn(Future(InsertOneResult.acknowledged(BsonNumber(1))))
      when(mockDraftEmailRepository.updateEmailSentStatus(*)).thenReturn(successful(draftEmail))
      when(mockSentEmailRepository.persist(*)).thenReturn(Future(InsertManyResult.unacknowledged()))
      val result = controller.sendEmail(emailUUID)(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 500" in new Setup {
      when(mockEmailService.sendEmail(emailUUID)).thenReturn(failed(new IOException("mongo error")))
//      when(mockDraftEmailRepository.getEmailData(*)).thenReturn(failed(new IOException("can not connect to email service")))
//      when(mockDraftEmailRepository.updateEmailSentStatus(*)).thenReturn(failed(new IOException("can not connect to mongo service")))
//      when(mockSentEmailRepository.persist(*)).thenReturn(failed(new IOException("can not connect to mongo service")))
      val result = controller.sendEmail(emailUUID)(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "POST /gatekeeper-email/save-email" should {
    "return 200 when email is saved successfully" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      when(mockEmailService.persistEmail(emailRequest, emailUUID)).thenReturn(successful(draftEmail))
      val result = controller2.saveEmail(emailUUID)(fakeSaveEmailRequest)
      status(result) shouldBe Status.OK
    }

    "return 500" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      when(mockEmailService.persistEmail(emailRequest, emailUUID)).thenReturn(failed(new IOException("can not connect to email service")))
      val result = controller2.saveEmail(emailUUID)(fakeSaveEmailRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return 400 when body is not JSON" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      val result = controller2.saveEmail(emailUUID)(fakeRequestWithBodyNotValidJson)
      status(result) shouldBe Status.BAD_REQUEST
      verifyNoMoreInteractions(mockEmailService)
    }

    "return 403 for InsufficientEnrolments" in new Setup {
      AuthConnectorMock.Authorise.thenReturnInsufficientEnrolments()
      val result = controller.saveEmail(emailUUID)(fakeSaveEmailRequest)
      status(result) shouldBe Status.FORBIDDEN
    }
  }

  "POST /gatekeeper-email/delete-email" should {
    "return 200" in new Setup {
      when(mockEmailService.deleteEmail(emailUUID)).thenReturn(successful(true))
      val result = controller2.deleteEmail(emailUUID)(fakeDeleteEmailRequest)
      status(result) shouldBe Status.OK
    }
    "return 500" in new Setup {
      when(mockEmailService.deleteEmail(emailUUID)).thenReturn(failed(new IOException("can not connect to email service")))
      val result = controller2.deleteEmail(emailUUID)(fakeDeleteEmailRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "POST /gatekeeper-email/update-email" should {
    "return 200" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      when(mockEmailService.updateEmail(*, *)).thenReturn(successful(draftEmail))
      val result = controller2.updateEmail(emailUUID)(fakeSaveEmailRequest)
      status(result) shouldBe Status.OK
    }

    "return 500" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      when(mockEmailService.updateEmail(emailRequest, emailUUID)).thenReturn(failed(new IOException("can not connect to email service")))
      val result = controller2.updateEmail(emailUUID)(fakeSaveEmailRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return 400" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      when(mockEmailService.updateEmail(emailRequest, emailUUID)).thenReturn(successful(draftEmail))
      val result = controller2.updateEmail(emailUUID)(fakeRequestWithBodyNotValidJson)
      status(result) shouldBe Status.BAD_REQUEST
    }

    "return 403 when authentication has insufficient enrolmwents" in new Setup {
      AuthConnectorMock.Authorise.thenReturnInsufficientEnrolments()
      val result = controller2.updateEmail(emailUUID)(fakeSaveEmailRequest)
      status(result) shouldBe Status.FORBIDDEN
    }
  }

  "POST /gatekeeperemail/updatefiles" should {
    "return 200" in new Setup {
      val toLocation = Path.File(Path.Directory("gatekeeper-email"), "file123")
      when(mockObjectStoreService.uploadToObjectStore(*,*,*))
        .thenReturn(successful(ObjectSummaryWithMd5(toLocation, 1024,
        Md5Hash("md5Hash"), Instant.parse("2018-04-24T09:30:00Z"))))

      when(mockEmailService.fetchEmail(emailUUIDToAttachFile)).thenReturn(successful(draftEmail))
      val result = controller2.updateFiles()(fakeRequestToUpdateFiles)
      verify(mockObjectStoreService).uploadToObjectStore(*, *, *)
      status(result) shouldBe Status.NO_CONTENT
    }

    "return 500" in new Setup {
      when(mockEmailService.fetchEmail(emailUUIDToAttachFile)).thenReturn(failed(new IOException("can not connect to email service")))
      val result = controller2.updateFiles()(fakeRequestToUpdateFiles)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "Upload to object store and update attachments" in new Setup {
      val uploadedFile345: UploadedFileWithObjectStore = UploadedFileWithObjectStore("Ref345", "/gatekeeper/downloadUrl/345", "", "", "file345", "",
        1024, cargo, None, None, Some(s"/gatekeeper/$emailUUIDToAttachFile"), None)
      val uploadedFile567: UploadedFileWithObjectStore = UploadedFileWithObjectStore("Ref567", "/gatekeeper/downloadUrl/567", "", "", "file567", "",
        1024, cargo, None, None, Some(s"/gatekeeper/$emailUUIDToAttachFile"), None)
      val uploadedFiles = uploadedFileMetadata.copy(uploadedFiles =
        Seq(uploadedFile123, uploadedFile345, uploadedFile567), cargo = cargo)
      private val fakeRequestToUpdateFiles = FakeRequest("POST", "/gatekeeperemail/updatefiles")
        .withHeaders("Content-Type" -> "application/json")
        .withBody(Json.toJson(uploadedFiles))
      val existingEmail = draftEmail.copy(emailUUID = emailUUIDToAttachFile, attachmentDetails = Some(Seq(uploadedFile123, uploadedFile345)))
      when(mockEmailService.fetchEmail(emailUUIDToAttachFile)).thenReturn(successful(existingEmail))

      val toLocation = Path.File(Path.Directory("gatekeeper-email"), "file567")
      when(mockObjectStoreService.uploadToObjectStore(emailUUIDToAttachFile, "/gatekeeper/downloadUrl/567", "file567"))
        .thenReturn(successful(ObjectSummaryWithMd5(toLocation, 1024, Md5Hash("md5Hash"), Instant.parse("2018-04-24T09:30:00Z"))))

      val captor = ArgumentCaptor.forClass(classOf[EmailRequest])
      await(controller2.updateFiles()(fakeRequestToUpdateFiles))
      verify(mockObjectStoreService, times(1)).uploadToObjectStore(emailUUIDToAttachFile, "/gatekeeper/downloadUrl/567", "file567")
      verify(mockEmailService).updateEmail(captor.capture(), anyString())
      val emailRequestCaptured: EmailRequest = captor.getValue
      emailRequestCaptured.attachmentDetails.get.size shouldBe 3
    }

    "Remove from object store and update attachments" in new Setup {
      val uploadedFile345: UploadedFileWithObjectStore = UploadedFileWithObjectStore("Ref345", "/gatekeeper/downloadUrl/345", "", "", "file345", "",
        1024, cargo, None, None, Some(s"/gatekeeper/$emailUUIDToAttachFile"), None)
      val uploadedFiles = uploadedFileMetadata.copy(uploadedFiles = Seq(uploadedFile345))
      private val fakeRequestToUpdateFiles = FakeRequest("POST", "/gatekeeperemail/updatefiles")
        .withHeaders("Content-Type" -> "application/json")
        .withBody(Json.toJson(uploadedFiles))

      val captor = ArgumentCaptor.forClass(classOf[EmailRequest])
      val existingEmail = draftEmail.copy(emailUUID = emailUUIDToAttachFile,
        attachmentDetails = Some(Seq(uploadedFile123, uploadedFile345)))
      when(mockEmailService.fetchEmail(emailUUIDToAttachFile)).thenReturn(successful(existingEmail))
      when(mockObjectStoreService.deleteFromObjectStore(emailUUIDToAttachFile, "file123")).thenReturn(successful())
      await(controller2.updateFiles()(fakeRequestToUpdateFiles))
      verify(mockObjectStoreService, times(1)).deleteFromObjectStore(emailUUIDToAttachFile, "file123")
      verify(mockEmailService).updateEmail(captor.capture(), anyString())
      val emailRequestCaptured: EmailRequest = captor.getValue
      emailRequestCaptured.attachmentDetails.get.size shouldBe 1
    }
  }

  "GET /gatekeeper-email/fetch-email" should {
      "return 200" in new Setup {
        when(mockEmailService.fetchEmail(emailUUID)).thenReturn(successful(draftEmail))
        val result = controller2.fetchEmail(emailUUID)(fakeRequest)
        status(result) shouldBe Status.OK
      }
    }
}
