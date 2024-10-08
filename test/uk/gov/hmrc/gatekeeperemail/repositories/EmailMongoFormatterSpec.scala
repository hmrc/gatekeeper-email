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

package uk.gov.hmrc.gatekeeperemail.repositories

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import play.api.libs.json.{JsObject, JsString}
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

import uk.gov.hmrc.gatekeeperemail.models.EmailStatus.SENT
import uk.gov.hmrc.gatekeeperemail.models.requests.DevelopersEmailQuery
import uk.gov.hmrc.gatekeeperemail.models.{DraftEmail, EmailTemplateData}

class EmailMongoFormatterSpec extends AnyWordSpec with Matchers with MockitoSugar with ArgumentMatchersSugar with FixedClock {

  "format" should {
    val formatter = DraftEmail.format

    "correctly write a Email message" in {
      val data: EmailTemplateData = EmailTemplateData("gatekeeper", Map(), false, Map(), None)
      val emailPreferences        = DevelopersEmailQuery()

      val email = DraftEmail(
        "61e00e08ed2f2471ce3126db",
        data,
        "DL Team",
        emailPreferences,
        "markdownEmailBody",
        "This is test email",
        "test subject",
        SENT,
        "composedBy",
        Some("approvedBy"),
        instant,
        2
      )

      val msgJson: JsObject = formatter.writes(email)
      msgJson.values.size shouldBe 12
      msgJson.value.get("recipientTitle") shouldBe Some(JsString("DL Team"))
    }
  }
}
