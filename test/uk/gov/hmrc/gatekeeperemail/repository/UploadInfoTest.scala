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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

import uk.gov.hmrc.gatekeeperemail.models.{InProgress, Reference, UploadedSuccessfully}
import uk.gov.hmrc.gatekeeperemail.repositories.UploadInfo

class UploadInfoTest extends AnyWordSpec with Matchers with FixedClock {

  "Serialization and deserialization of UploadDetails" should {

    "serialize and deserialize InProgress status" in {
      val input = UploadInfo(Reference("ABC"), InProgress, instant)

      val serialized = UploadInfo.format.writes(input)
      val output     = UploadInfo.format.reads(serialized)

      output.get.createDateTime should equal(input.createDateTime)
      output.get.status should equal(input.status)
      output.get.reference should equal(input.reference)
    }

    "serialize and deserialize Failed status" in {

      val input = UploadInfo(Reference("ABC"), InProgress, instant)

      val serialized = UploadInfo.format.writes(input)
      val output     = UploadInfo.format.reads(serialized)
      output.get.createDateTime should equal(input.createDateTime)
      output.get.status should equal(input.status)
      output.get.reference should equal(input.reference)
    }

    "serialize and deserialize UploadedSuccessfully status when size is unknown" in {
      val input = UploadInfo(
        Reference("ABC"),
        UploadedSuccessfully("foo.txt", "text/plain", "http:localhost:8080", size = None, "http://aws.s3.object-store-url"),
        instant
      )

      val serialized = UploadInfo.format.writes(input)
      val output     = UploadInfo.format.reads(serialized)

      output.get.createDateTime should equal(input.createDateTime)
      output.get.status should equal(input.status)
      output.get.reference should equal(input.reference)
    }

    "serialize and deserialize UploadedSuccessfully status when size is known" in {

      val input = UploadInfo(
        Reference("ABC"),
        UploadedSuccessfully("foo.txt", "text/plain", "http:localhost:8080", size = Some(123456), "http://aws.s3.object-store-url"),
        instant
      )

      val serialized = UploadInfo.format.writes(input)
      val output     = UploadInfo.format.reads(serialized)

      output.get.createDateTime should equal(input.createDateTime)
      output.get.status should equal(input.status)
      output.get.reference should equal(input.reference)
    }
  }
}
