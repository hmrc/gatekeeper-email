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

import play.api.libs.json.Json
import uk.gov.hmrc.gatekeeperemail.models.DraftEmail
import java.time.Instant

trait RawEmailTestData {
  def dateToJsonObj(date: Instant) = Json.obj(f"$$date" -> date.toEpochMilli)

  def fromDraftEmail(draftEmail: DraftEmail) = {
    // val localDateTimeNow = LocalDateTime.now()
    val createDateTimeString: String = "2023-09-05T12:08:24.125597"
    

    Json.obj(
      "emailUUID" -> draftEmail.emailUUID,
      "templateData" -> draftEmail.templateData,
      "recipientTitle" -> draftEmail.recipientTitle,
      "userSelectionQuery" -> draftEmail.userSelectionQuery,
      "attachmentDetails" -> draftEmail.attachmentDetails,
      "markdownEmailBody" -> draftEmail.markdownEmailBody,
      "htmlEmailBody" -> draftEmail.htmlEmailBody,
      "subject" -> draftEmail.subject,
      "status" -> draftEmail.status,
      "composedBy" -> draftEmail.composedBy,
      "approvedBy" -> draftEmail.approvedBy,
      "createDateTime" -> createDateTimeString,
      "emailsCount" -> draftEmail.emailsCount
    )
  }
}

object RawEmailTestData extends RawEmailTestData {

}
