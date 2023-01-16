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

package uk.gov.hmrc.gatekeeperemail.repository

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.gatekeeperemail.models.{InProgress, Reference, UploadedSuccessfully}
import uk.gov.hmrc.gatekeeperemail.repositories.UploadInfo

class UploadInfoTest extends AnyWordSpec with Matchers {

  "Serialization and deserialization of UploadDetails" should {

    "serialize and deserialize InProgress status" in {

      val dateTime: LocalDateTime = LocalDateTime.parse("02/02/2022 20:27:05", DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
      val input                   = UploadInfo(Reference("ABC"), InProgress, dateTime)

      val serialized = UploadInfo.format.writes(input)
      val output     = UploadInfo.format.reads(serialized)

      output.get.createDateTime should equal(input.createDateTime)
      output.get.status should equal(input.status)
      output.get.reference should equal(input.reference)
    }

    "serialize and deserialize Failed status" in {
      val dateTime: LocalDateTime = LocalDateTime.parse("02/02/2022 20:27:05", DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
      val input                   = UploadInfo(Reference("ABC"), InProgress, dateTime)

      val serialized = UploadInfo.format.writes(input)
      val output     = UploadInfo.format.reads(serialized)
      output.get.createDateTime should equal(input.createDateTime)
      output.get.status should equal(input.status)
      output.get.reference should equal(input.reference)
    }

    "serialize and deserialize UploadedSuccessfully status when size is unknown" in {
      val dateTime: LocalDateTime = LocalDateTime.parse("02/02/2022 20:27:05", DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
      val input                   = UploadInfo(
        Reference("ABC"),
        UploadedSuccessfully("foo.txt", "text/plain", "http:localhost:8080", size = None, "http://aws.s3.object-store-url"),
        dateTime
      )

      val serialized = UploadInfo.format.writes(input)
      val output     = UploadInfo.format.reads(serialized)

      output.get.createDateTime should equal(input.createDateTime)
      output.get.status should equal(input.status)
      output.get.reference should equal(input.reference)
    }

    "serialize and deserialize UploadedSuccessfully status when size is known" in {
      val dateTime: LocalDateTime = LocalDateTime.parse("02/02/2022 20:27:05", DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

      val input = UploadInfo(
        Reference("ABC"),
        UploadedSuccessfully("foo.txt", "text/plain", "http:localhost:8080", size = Some(123456), "http://aws.s3.object-store-url"),
        dateTime
      )

      val serialized = UploadInfo.format.writes(input)
      val output     = UploadInfo.format.reads(serialized)

      output.get.createDateTime should equal(input.createDateTime)
      output.get.status should equal(input.status)
      output.get.reference should equal(input.reference)
    }
  }
}
