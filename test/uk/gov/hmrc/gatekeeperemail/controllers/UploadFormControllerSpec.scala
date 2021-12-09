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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.{FakeRequest, StubControllerComponentsFactory, StubPlayBodyParsersFactory}
import uk.gov.hmrc.gatekeeperemail.common.AsyncHmrcSpec
import uk.gov.hmrc.gatekeeperemail.models.JsonFormatters._
import uk.gov.hmrc.gatekeeperemail.models.{Failed, Reference, UploadId, UploadStatus, UploadedFailedWithErrors, UploadedSuccessfully}
import uk.gov.hmrc.gatekeeperemail.services.FileUploadStatusService

import scala.concurrent.ExecutionContext.Implicits.global
import java.util.UUID.randomUUID
import scala.concurrent.Future.successful

class UploadFormControllerSpec extends AsyncHmrcSpec with AnyWordSpec
  with Matchers with GuiceOneAppPerSuite
  with StubControllerComponentsFactory
  with StubPlayBodyParsersFactory {

  implicit lazy val materializer: Materializer = mock[Materializer]

  trait Setup {
    val mockFileUploadStatusService: FileUploadStatusService = mock[FileUploadStatusService]
    val controllerComponents: ControllerComponents = stubControllerComponents()

    val underTest = new UploadFormController(mockFileUploadStatusService, controllerComponents,
      stubPlayBodyParsers(materializer))
  implicit lazy val request = FakeRequest()

    when(mockFileUploadStatusService.requestUpload(UploadId(randomUUID), Reference(randomUUID.toString))).thenReturn(successful("String"))
  }
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"     -> false,
        "metrics.enabled" -> false
      )
      .build()

  private val fakeRequest = FakeRequest("POST", s"/gatekeeper-email/insertfileuploadstatus?key=${randomUUID}")

  private val controller = app.injector.instanceOf[UploadFormController]

  "UploadFormController should" {

    "be able to insert a FileUploadStatus Record" in {
      val result = controller.addUploadedFileStatus(key)
    }
  }

  "UploadFormController BODY JSON reader" should {

    "be able to serialize successful body" in {

      val body =
        """
          |{
          |        "name" : "abc.txt",
          |        "mimeType" : "pdf",
          |        "downloadUrl" : "http://abcs3",
          |        "size" : 1234,
          |        "_type" : "UploadedSuccessfully"
          |}
        """.stripMargin

      val result = Json.parse(body).as[UploadStatus]
     result shouldBe UploadedSuccessfully("abc.txt", "pdf", "http://abcs3", Some(1234))


    }

    "be able to serialize UploadedFailedWithErrors body" in {

      val body =
        """
          |{
          |        "errorCode" : "999",
          |        "errorMessage" : "Wrong Size",
          |        "errorRequestId" : "fedf34r343",
          |        "key" : "1234",
          |        "_type" : "UploadedFailedWithErrors"
          |}
        """.stripMargin

      val result = Json.parse(body).as[UploadStatus]
      result shouldBe UploadedFailedWithErrors("999", "Wrong Size", "fedf34r343", "1234")


    }

    "be able to serialise faied body" in {

      val body =
        """
          |{
          |        "_type" : "Failed"
          |
          |}
          |""".stripMargin

      val result = Json.parse(body).as[UploadStatus]
      result shouldBe Failed
    }
  }
}
