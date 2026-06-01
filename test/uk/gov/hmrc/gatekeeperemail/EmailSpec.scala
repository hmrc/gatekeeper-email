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

import uk.gov.hmrc.gatekeeperemail.models.EmailStatus

class EmailSpec extends BaseJsonFormattersSpec with TableDrivenPropertyChecks {

  "email status" when {
    val values =
      Table(
        ("Type", "text"),
        (EmailStatus.Failed, "failed"),
        (EmailStatus.Pending, "pending"),
        (EmailStatus.Sent, "sent")
      )

    "convert lower case string to case object" in {
      forAll(values) { (s, t) =>
        EmailStatus.apply(t) shouldBe Some(s)
        EmailStatus.unsafeApply(t) shouldBe s
      }
    }

    "convert mixed case string to case object" in {
      forAll(values) { (s, t) =>
        EmailStatus.apply(t.toUpperCase()) shouldBe Some(s)
        EmailStatus.unsafeApply(t.toUpperCase()) shouldBe s
      }
    }

    "convert string value to None when undefined or empty" in {
      EmailStatus.apply("rubbish") shouldBe None
      EmailStatus.apply("") shouldBe None
    }

    "throw when string value is invalid" in {
      intercept[RuntimeException] {
        EmailStatus.unsafeApply("rubbish")
      }.getMessage() should include("Email Status")
    }

    "read with error from Json" in {
      intercept[Exception] {
        testFromJson[EmailStatus](s"""123""")(EmailStatus.Failed)
      }.getMessage() should include("Cannot parse Email Status from '123'")
    }

    val jsonValues =
      Table(
        ("Type", "text"),
        (EmailStatus.Failed, "FAILED"),
        (EmailStatus.Sent, "SENT"),
        (EmailStatus.Pending, "PENDING")
      )

    "write to Json" in {
      forAll(jsonValues) { (s, t) =>
        Json.toJson[EmailStatus](s) shouldBe JsString(t)
      }
    }

    "read from Json" in {
      forAll(jsonValues) { (s, t) =>
        testFromJson[EmailStatus](s""" "$t" """)(s)
      }
    }
  }
}
