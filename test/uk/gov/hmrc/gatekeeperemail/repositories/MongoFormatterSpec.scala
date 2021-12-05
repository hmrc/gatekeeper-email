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

package uk.gov.hmrc.gatekeeperemail.repositories

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsObject, JsString}
import uk.gov.hmrc.gatekeeperemail.models.Email


class MongoFormatterSpec extends AnyWordSpec with Matchers with MockitoSugar with ArgumentMatchersSugar {

  "format" should {
    val formatter = MongoFormatter.emailFormatter
    "correctly write a Email message" in {
      val email = Email("DL Team", List("test@digital.hmrc.gov.uk"), None, "markdownEmailBody", Some("This is test email"),
        "test subject", "composedBy", Some("approvedBy"), DateTime.now(UTC))
      val msgJson: JsObject = formatter.writes(email)
      msgJson.values.size shouldBe 8
      msgJson.value.get("recepientTitle") shouldBe Some(JsString("DL Team"))
    }
  }
}