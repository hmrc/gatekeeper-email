/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeperemail

import org.scalatest.prop.TableDrivenPropertyChecks

import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.apiplatform.modules.common.utils.BaseJsonFormattersSpec

import uk.gov.hmrc.gatekeeperemail.models.TopicOptionChoice

class TopicOptionChoiceSpec extends BaseJsonFormattersSpec with TableDrivenPropertyChecks {

  "email status" when {
    val values =
      Table(
        ("Type", "text"),
        (TopicOptionChoice.BusinessAndPolicy, "businessandpolicy"),
        (TopicOptionChoice.EventInvites, "eventinvites"),
        (TopicOptionChoice.ReleaseSchedules, "releaseschedules"),
        (TopicOptionChoice.Technical, "technical")
      )

    "convert lower case string to case object" in {
      forAll(values) { (s, t) =>
        TopicOptionChoice.apply(t) shouldBe Some(s)
        TopicOptionChoice.unsafeApply(t) shouldBe s
      }
    }

    "convert mixed case string to case object" in {
      forAll(values) { (s, t) =>
        TopicOptionChoice.apply(t.toUpperCase()) shouldBe Some(s)
        TopicOptionChoice.unsafeApply(t.toUpperCase()) shouldBe s
      }
    }

    "convert string value to None when undefined or empty" in {
      TopicOptionChoice.apply("rubbish") shouldBe None
      TopicOptionChoice.apply("") shouldBe None
    }

    "throw when string value is invalid" in {
      intercept[RuntimeException] {
        TopicOptionChoice.unsafeApply("rubbish")
      }.getMessage() should include("Topic Option Choice")
    }

    "read with error from Json" in {
      intercept[Exception] {
        testFromJson[TopicOptionChoice](s"""123""")(TopicOptionChoice.BusinessAndPolicy)
      }.getMessage() should include("Cannot parse Topic Option Choice from '123'")
    }

    val jsonValues =
      Table(
        ("Type", "text"),
        (TopicOptionChoice.BusinessAndPolicy, "BUSINESS_AND_POLICY"),
        (TopicOptionChoice.EventInvites, "EVENT_INVITES"),
        (TopicOptionChoice.ReleaseSchedules, "RELEASE_SCHEDULES"),
        (TopicOptionChoice.Technical, "TECHNICAL")
      )

    "write to Json" in {
      forAll(jsonValues) { (s, t) =>
        Json.toJson[TopicOptionChoice](s) shouldBe JsString(t)
      }
    }

    "read from Json" in {
      forAll(jsonValues) { (s, t) =>
        testFromJson[TopicOptionChoice](s""" "$t" """)(s)
      }
    }
  }
}
