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
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.{FakeRequest, StubControllerComponentsFactory, StubPlayBodyParsersFactory}
import common.AsyncHmrcSpec
import uk.gov.hmrc.gatekeeperemail.models.JsonFormatters._
import uk.gov.hmrc.gatekeeperemail.models.{Failed, InProgress, Reference, UploadId, UploadStatus, UploadedFailedWithErrors, UploadedSuccessfully}
import uk.gov.hmrc.gatekeeperemail.services.{FileUploadStatusService, UploadProgressTracker}
import play.api.test.Helpers.{contentAsJson, status}
import uk.gov.hmrc.gatekeeperemail.repository.UploadInfo

import scala.concurrent.ExecutionContext.Implicits.global
import java.util.UUID.randomUUID
import scala.concurrent.Future
import scala.concurrent.Future.successful

class UploadFormControllerSpec extends AsyncHmrcSpec  with GuiceOneAppPerSuite
  with StubControllerComponentsFactory
  with StubPlayBodyParsersFactory {

  val uploadId = UploadId(randomUUID)
  val reference = randomUUID.toString
  val uploadInfo1 = UploadInfo(uploadId, Reference(reference), UploadedSuccessfully("2e2e", "pdf", "dwsdwe", Some(123)))

  implicit lazy val materializer: Materializer = mock[Materializer]

  trait Setup {
    val mockFileUploadStatusService: FileUploadStatusService = mock[FileUploadStatusService]
    val controllerComponents: ControllerComponents = stubControllerComponents()
    val underTest = new UploadFormController(mockFileUploadStatusService, controllerComponents, stubPlayBodyParsers(materializer))
    implicit lazy val request = FakeRequest()
    when(mockFileUploadStatusService.requestUpload(reference)).thenReturn(successful(uploadInfo1))
  }

  "UploadFormController" should {

    "be able to insert a FileUploadStatus Record" in  new Setup {
      val result = underTest.addUploadedFileStatus(reference)(request)
      status(result) shouldBe OK
      contentAsJson(result) shouldEqual Json.toJson(uploadInfo1)
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
