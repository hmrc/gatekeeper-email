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

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.gatekeeperemail.models.Reference

import java.net.URL
import java.time.Instant

class CallbackBodyTest extends WordSpec with Matchers {

  "CallbackBody JSON reader" should {
    "be able to deserialize successful body" in {

      val body =
        """
          |{
          |    "reference" : "11370e18-6e24-453e-b45a-76d3e32ea33d",
          |    "fileStatus" : "READY",
          |    "downloadUrl" : "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
          |    "uploadDetails": {
          |        "uploadTimestamp": "2018-04-24T09:30:00Z",
          |        "checksum": "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
          |        "fileName": "test.pdf",
          |        "fileMimeType": "application/pdf",
          |        "size": 45678
          |    }
          |}
          |
        """.stripMargin

      CallbackBody.reads.reads(Json.parse(body)) shouldBe
        JsSuccess(
          ReadyCallbackBody(
            reference = "11370e18-6e24-453e-b45a-76d3e32ea33d",
            downloadUrl = "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
            uploadDetails = UploadDetails(
              uploadTimestamp = Instant.parse("2018-04-24T09:30:00Z"),
              checksum = "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
              fileMimeType = "application/pdf",
              fileName = "test.pdf",
              size = 45678L
            )
          )
        )

    }

    "should be able to deserialize failed body" in {

      val body =
        """
          |{
          |    "reference" : "11370e18-6e24-453e-b45a-76d3e32ea33d",
          |    "fileStatus" : "FAILED",
          |    "failureDetails": {
          |        "failureReason": "QUARANTINE",
          |        "message": "e.g. This file has a virus"
          |    }
          |}
        """.stripMargin

      CallbackBody.reads.reads(Json.parse(body)) shouldBe
        JsSuccess(
          FailedCallbackBody(
            reference = "11370e18-6e24-453e-b45a-76d3e32ea33d",
            fileStatus = "FAILED",
            failureDetails = ErrorDetails(
              failureReason = "QUARANTINE",
              message = "e.g. This file has a virus"
            )
          ))

    }

    "be able to fail deserialize invalid json body" in {

      val body =
        """
          |{
          |    "reference" : "11370e18-6e24-453e-b45a-76d3e32ea33d",
          |    "fileStatus" : "STUCK",
          |    "downloadUrl" : "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
          |    "uploadDetails": {
          |        "uploadTimestamp": "2018-04-24T09:30:00Z",
          |        "checksum": "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
          |        "fileName": "test.pdf",
          |        "fileMimeType": "application/pdf",
          |        "size": 45678
          |    }
          |}
          |
        """.stripMargin

      CallbackBody.reads.reads(Json.parse(body)) shouldBe
        JsError("""Invalid file upload status type: "STUCK"""")
    }

    "be able to fail deserialize missing type in json body" in {

      val body =
        """
          |{
          |    "reference" : "11370e18-6e24-453e-b45a-76d3e32ea33d",
          |    "downloadUrl" : "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
          |    "uploadDetails": {
          |        "uploadTimestamp": "2018-04-24T09:30:00Z",
          |        "checksum": "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
          |        "fileName": "test.pdf",
          |        "fileMimeType": "application/pdf",
          |        "size": 45678
          |    }
          |}
          |
          """.stripMargin

      CallbackBody.reads.reads(Json.parse(body)) shouldBe
        JsError("Missing file upload status type")
    }
  }
}

