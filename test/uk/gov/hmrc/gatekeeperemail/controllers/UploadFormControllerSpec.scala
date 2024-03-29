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

import java.util.UUID.randomUUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

import org.apache.pekko.stream.Materializer
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.{JsResultException, Json}
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents}
import play.api.test.Helpers.{contentAsJson, contentAsString, status}
import play.api.test.{FakeRequest, StubControllerComponentsFactory, StubPlayBodyParsersFactory}
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

import uk.gov.hmrc.gatekeeperemail.common.AsyncHmrcTestSpec
import uk.gov.hmrc.gatekeeperemail.models.JsonFormatters._
import uk.gov.hmrc.gatekeeperemail.models._
import uk.gov.hmrc.gatekeeperemail.repositories.UploadInfo
import uk.gov.hmrc.gatekeeperemail.services.FileUploadStatusService

class UploadFormControllerSpec extends AsyncHmrcTestSpec with GuiceOneAppPerSuite
    with StubControllerComponentsFactory
    with StubPlayBodyParsersFactory with FixedClock {

  val uploadId            = UploadId(randomUUID)
  val reference           = randomUUID.toString
  val uploadStatusSuccess = UploadedSuccessfully("abc.txt", "pdf", "http://abcs3", Some(1234), "http://aws.s3.object-store-url")

  val uploadSuccesfulBody  =
    """{"name" : "abc.txt", "mimeType" : "pdf", "downloadUrl" : "http://abcs3",
      |"size" : 1234, "_type" : "UploadedSuccessfully", "objectStoreUrl": "http://aws.s3.object-store-url"}""".stripMargin
  val failedBody           = """{"_type" : "Failed"}"""
  val uploadInfo1          = UploadInfo(Reference(reference), uploadStatusSuccess, instant)
  val uploadInfoInProgress = UploadInfo(Reference(reference), InProgress, instant)
  val uploadInfoInFailed   = UploadInfo(Reference(reference), Failed, instant)

  implicit lazy val materializer: Materializer = mock[Materializer]

  trait Setup {
    val mockFileUploadStatusService: FileUploadStatusService = mock[FileUploadStatusService]
    val controllerComponents: ControllerComponents           = stubControllerComponents()
    val underTest                                            = new UploadFormController(mockFileUploadStatusService, controllerComponents, stubPlayBodyParsers(materializer))

    implicit lazy val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    when(mockFileUploadStatusService.requestUpload(reference)).thenReturn(successful(uploadInfoInProgress))
    when(mockFileUploadStatusService.registerUploadResult(reference, uploadStatusSuccess)).thenReturn(successful(uploadInfo1))
    when(mockFileUploadStatusService.registerUploadResult(reference, Failed)).thenReturn(successful(uploadInfoInFailed))
    when(mockFileUploadStatusService.getUploadResult(Reference(reference))).thenReturn(successful(Some(uploadInfo1)))
  }

  "UploadFormController" should {

    "be able to insert a FileUploadStatus Record" in new Setup {
      val result = underTest.addUploadedFileStatus(reference)(request)
      status(result) shouldBe OK
      contentAsJson(result) shouldEqual Json.toJson(uploadInfoInProgress)
    }

    "be able to update a FileUploadStatus Record" in new Setup {
      val result = underTest.updateUploadedFileStatus(reference)(request.withBody(Json.parse(uploadSuccesfulBody)))
      status(result) shouldBe OK
      contentAsJson(result) shouldEqual Json.toJson(uploadInfo1)
    }

    "be able to update a FileUploadStatus Record as Failed" in new Setup {
      val result = underTest.updateUploadedFileStatus(reference)(request.withBody(Json.parse(failedBody)))
      status(result) shouldBe OK
      contentAsJson(result) shouldEqual Json.toJson(uploadInfoInFailed)
    }

    "be able to fetch a FileUploadStatus Record" in new Setup {
      val result = underTest.fetchUploadedFileStatus(reference)(request)
      status(result) shouldBe OK
      contentAsJson(result) shouldEqual Json.toJson(uploadInfo1)
    }

    "return 404 (not found) when the fileUploadStatus does not exist" in new Setup {

      when(mockFileUploadStatusService.getUploadResult(Reference("reference1"))).thenReturn(successful(None))

      val result = underTest.fetchUploadedFileStatus("reference1")(request)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) shouldBe "No uploadInfo found"

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
          |        "_type" : "UploadedSuccessfully",
          |        "objectStoreUrl" : "http://aws.s3.object-store-url"
          |}
        """.stripMargin

      val result = Json.parse(body).as[UploadStatus]
      result shouldBe UploadedSuccessfully("abc.txt", "pdf", "http://abcs3", Some(1234), "http://aws.s3.object-store-url")

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

    "be able to serialise InProgress body" in {

      val body =
        """
          |{
          |        "_type" : "InProgress"
          |
          |}
          |""".stripMargin

      val result = Json.parse(body).as[UploadStatus]
      result shouldBe InProgress
    }

    "be able to serialise failed body" in {

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

    "be not able to serialise unknown type" in {

      val body                         =
        """
          |{
          |        "_type" : "UNKNOWN"
          |
          |}
          |""".stripMargin
      val exception: JsResultException = intercept[JsResultException] {
        Json.parse(body).as[UploadStatus]
      }
      exception.getMessage.contains("Unexpected value of _type: UNKNOWN")
    }

    "be able to not serialise when no type" in {

      val body                         =
        """
          |{
          |        "typ" : "UNKNOWN"
          |
          |}
          |""".stripMargin
      val exception: JsResultException = intercept[JsResultException] {
        Json.parse(body).as[UploadStatus]
      }
      exception.getMessage.contains("""Unexpected value of _type: \"UNKNOWN\"""")
    }
  }
}
