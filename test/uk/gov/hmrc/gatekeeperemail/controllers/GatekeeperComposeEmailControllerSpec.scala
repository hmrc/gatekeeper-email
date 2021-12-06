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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.Play.materializer
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.gatekeeperemail.models.SendEmailRequest

class GatekeeperComposeEmailControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"     -> false,
        "metrics.enabled" -> false
      )
      .build()
  val parameters: Map[String, String] = Map("subject" -> s"$subject",
    "fromAddress" -> "gateKeeper",
    "body" -> "Body to be used in the email template",
    "service" -> "gatekeeper")

  val emailId = "email@example.com"
  val subject = "Email subject"
  val emailRequest = SendEmailRequest(List(emailId), "gatekeeper", parameters)
  private val fakeRequest = FakeRequest("POST", "/gatekeeper-email").withBody(Json.toJson(emailRequest))

  private val controller = app.injector.instanceOf[GatekeeperComposeEmailController]

  "POST /" should {
    "return 200" in {
      val result = controller.sendEmail(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }
}
