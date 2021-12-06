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
import play.api.libs.json.Json
import uk.gov.hmrc.gatekeeperemail.models.JsonFormatters._
import uk.gov.hmrc.gatekeeperemail.models.{Failed, UploadStatus, UploadedFailedWithErrors, UploadedSuccessfully}

class UploadFormControllerSpec extends WordSpec with Matchers {

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
