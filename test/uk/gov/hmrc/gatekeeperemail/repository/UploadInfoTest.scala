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

package uk.gov.hmrc.gatekeeperemail.repository

import org.scalatest.{Matchers, WordSpec}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.gatekeeperemail.connectors.Reference
import uk.gov.hmrc.gatekeeperemail.models.{Failed, InProgress, UploadId, UploadedSuccessfully}

class UploadInfoTest extends WordSpec with Matchers {

  "Serialization and deserialization of UploadDetails" should {

    "serialize and deserialize InProgress status" in {
      val input = UploadInfo(BSONObjectID.generate(), UploadId.generate, Reference("ABC"), InProgress)

      val serialized = UploadInfo.format.writes(input)
      val output = UploadInfo.format.reads(serialized)

      output.get shouldBe input
    }

    "serialize and deserialize Failed status" in {
      val input = UploadInfo(BSONObjectID.generate(), UploadId.generate, Reference("ABC"), Failed)

      val serialized = UploadInfo.format.writes(input)
      val output = UploadInfo.format.reads(serialized)

      output.get shouldBe input
    }

    "serialize and deserialize UploadedSuccessfully status when size is unknown" in {
      val input = UploadInfo(
        BSONObjectID.generate(),
        UploadId.generate,
        Reference("ABC"),
        UploadedSuccessfully("foo.txt", "text/plain", "http:localhost:8080", size = None)
      )

      val serialized = UploadInfo.format.writes(input)
      val output = UploadInfo.format.reads(serialized)

      output.get shouldBe input
    }

    "serialize and deserialize UploadedSuccessfully status when size is known" in {
      val input = UploadInfo(
        BSONObjectID.generate(),
        UploadId.generate,
        Reference("ABC"),
        UploadedSuccessfully("foo.txt", "text/plain", "http:localhost:8080", size = Some(123456))
      )

      val serialized = UploadInfo.format.writes(input)
      val output = UploadInfo.format.reads(serialized)

      output.get shouldBe input
    }
  }
}
