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

import java.time.{Instant, LocalDateTime, ZonedDateTime}
import java.util.UUID.randomUUID

import akka.stream.Materializer
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.Helpers.{contentAsJson, status}
import play.api.test.{FakeRequest, StubControllerComponentsFactory, StubPlayBodyParsersFactory}
import uk.gov.hmrc.gatekeeperemail.common.{AsyncHmrcSpec, AsyncHmrcTestSpec}
import uk.gov.hmrc.gatekeeperemail.models.JsonFormatters._
import uk.gov.hmrc.gatekeeperemail.models.{Reference, UploadId, UploadedFailedWithErrors, UploadedSuccessfully}
import uk.gov.hmrc.gatekeeperemail.repositories.UploadInfo
import uk.gov.hmrc.gatekeeperemail.services.UpscanCallbackService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

class UploadCallbackControllerSpec extends AsyncHmrcTestSpec  with GuiceOneAppPerSuite
  with StubControllerComponentsFactory
  with StubPlayBodyParsersFactory {

  val uploadId = UploadId(randomUUID)
  val reference = randomUUID.toString
  val readyCallbackJsonbody =
    """{"reference" : "11370e18-6e24-453e-b45a-76d3e32ea33d", "fileStatus" : "READY",
          "downloadUrl" : "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
          "uploadDetails": {
              "uploadTimestamp": "2018-04-24T09:30:00Z",
              "checksum": "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              "fileName": "test.pdf",
              "fileMimeType": "application/pdf",
              "size": 45678
          }
      }""".stripMargin
  val readyCallbackBody = ReadyCallbackBody(
    reference = "11370e18-6e24-453e-b45a-76d3e32ea33d",
    downloadUrl = "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
    uploadDetails = UploadDetails(
      uploadTimestamp = Instant.parse("2018-04-24T09:30:00Z"),
      checksum = "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
      fileMimeType = "application/pdf",
      fileName = "test.pdf",
      size = 45678L
    ))
  val uploadStatusSuccess = UploadedSuccessfully("test.pdf", "pdf", "https://bucketName.s3.eu-west-2.amazonaws.com?1235676", Some(45678L), "http://aws.s3.object-store-url")
  val uploadStatusSFailedWithErrors = UploadedFailedWithErrors("FAILED", "There is Virus", "1234567", reference)

  val failedCallbackBody =         FailedCallbackBody(
    reference = "11370e18-6e24-453e-b45a-76d3e32ea33d",
    fileStatus = "FAILED",
    failureDetails = ErrorDetails(
      failureReason = "QUARANTINE",
      message = "e.g. This file has a virus"
    )
  )
  val failedCallbackBodyJson = """
                                 |{
                                 |    "reference" : "11370e18-6e24-453e-b45a-76d3e32ea33d",
                                 |    "fileStatus" : "FAILED",
                                 |    "failureDetails": {
                                 |        "failureReason": "QUARANTINE",
                                 |        "message": "e.g. This file has a virus"
                                 |    }
                                 |}
        """.stripMargin

  val uploadInfoSuccess = UploadInfo(Reference(reference), uploadStatusSuccess, LocalDateTime.now())
  val uploadInfoFailed = UploadInfo(Reference(reference), uploadStatusSFailedWithErrors, LocalDateTime.now())

  implicit lazy val materializer: Materializer = mock[Materializer]

  trait Setup {
    val mockUpscanCallbackService: UpscanCallbackService = mock[UpscanCallbackService]
    val controllerComponents: ControllerComponents = stubControllerComponents()
    val underTest = new UploadCallbackController(mockUpscanCallbackService, controllerComponents)
    implicit lazy val request = FakeRequest()
    when(mockUpscanCallbackService.handleCallback(readyCallbackBody)).thenReturn(successful(uploadInfoSuccess))
    when(mockUpscanCallbackService.handleCallback(failedCallbackBody)).thenReturn(successful(uploadInfoFailed))
  }

  "UploadCallbackController" should {

    "be able to handle a readyCallbackbody call" in new Setup {
      val result = underTest.callback()(request.withBody(Json.parse(readyCallbackJsonbody)))
      status(result) shouldBe OK
      contentAsJson(result) shouldEqual Json.toJson(uploadInfoSuccess)
    }

    "be able to handle a failedCallbackbody call" in new Setup {
      val result = underTest.callback()(request.withBody(Json.parse(failedCallbackBodyJson)))
      status(result) shouldBe OK
      contentAsJson(result) shouldEqual Json.toJson(uploadInfoFailed)
    }
  }
}
