/*
 * Copyright 2021 HM Revenue & Customs
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
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.PlayBodyParsers
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.gatekeeperemail.connectors.GatekeeperEmailConnector
import uk.gov.hmrc.gatekeeperemail.models.{EmailData, EmailRequest}
import uk.gov.hmrc.gatekeeperemail.services.EmailService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

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

  val emailRequest = EmailRequest(List(emailId), "gatekeeper", EmailData(emailId, subject, emailBody))
  private val fakeRequest = FakeRequest("POST", "/").withBody(Json.toJson(emailRequest))
  lazy implicit val mat: Materializer = app.materializer
  private val playBodyParsers: PlayBodyParsers = app.injector.instanceOf[PlayBodyParsers]
  private val emailService: EmailService = app.injector.instanceOf[EmailService]

  val mockEmailConnector: GatekeeperEmailConnector = mock[GatekeeperEmailConnector]
  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[GatekeeperEmailConnector].to(mockEmailConnector))
//    .overrides(bind[EmailService].to(emailService))
    .configure("metrics.enabled" -> false, "auditing.enabled" -> false)
    .build()

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val controller = new GatekeeperComposeEmailController(Helpers.stubMessagesControllerComponents(),
      mockEmailConnector,
      playBodyParsers, emailService)
  }
  "POST /" should {
    "return 200" in new Setup {
      when(mockEmailConnector.sendEmail(*)(*)).thenReturn(
        successful(Json.obj("matchFound" -> false, "availableForVerification" -> false)))

      val result = controller.sendEmail(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }
}
