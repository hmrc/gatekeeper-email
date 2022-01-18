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
import uk.gov.hmrc.gatekeeperemail.models.{EmailData, EmailRequest, RenderResult}
import uk.gov.hmrc.gatekeeperemail.repositories.EmailRepository
import uk.gov.hmrc.gatekeeperemail.services.EmailService
import uk.gov.hmrc.http.HeaderCarrier

import java.io.IOException
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

  val emailRequest = EmailRequest(List(emailId), "gatekeeper", EmailData(emailId, subject, emailBody))
  private val fakeRequest = FakeRequest("POST", "/gatekeeper-email").withBody(Json.toJson(emailRequest))
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
    val controller = new GatekeeperComposeEmailController(Helpers.stubMessagesControllerComponents(),
      playBodyParsers, emailService)


    when(emailRendererConnectorMock.getTemplatedEmail(*))
      .thenReturn(successful(Right(RenderResult("RGVhciB1c2VyLCBUaGlzIGlzIGEgdGVzdCBtYWls",
        "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA==", "from@digital.hmrc.gov.uk", "subject", ""))))

  }
  "POST /gatekeeper-email" should {
    "return 200" in new Setup {
      when(mockEmailConnector.sendEmail(*)).thenReturn(successful(200))
      when(mockEmailRepository.persist(*)).thenReturn(Future(InsertOneResult.acknowledged(BsonNumber(1))))
      val result = controller.sendEmail(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 400 if json parse fails" in new Setup {
      val message: JsObject = Json.obj("to" -> "test@digital.hmrc.gov.uk", "templateId"-> "gatekeeper",
        "emailData" -> Json.obj("emailRecipient" -> "test@digital.hmrc.gov.uk", "emailSubject" -> "test subject",
          "emailBody" -> "test email"))
      val result = controller.sendEmail()(fakeRequest.withBody(message))
      status(result) shouldBe Status.BAD_REQUEST
    }

    "return 500" in new Setup {
      when(mockEmailConnector.sendEmail(*)).thenReturn(failed(new IOException("can not connect to email service")))
      when(mockEmailRepository.persist(*)).thenReturn(Future(InsertOneResult.acknowledged(BsonNumber(1))))
      val result = controller.sendEmail(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }
}
