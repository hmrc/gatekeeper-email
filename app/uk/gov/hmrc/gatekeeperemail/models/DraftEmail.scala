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

package uk.gov.hmrc.gatekeeperemail.models

import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import uk.gov.hmrc.gatekeeperemail.models.requests.DevelopersEmailQuery
import java.time.Instant

case class EmailTemplateData(
    templateId: String,
    parameters: Map[String, String],
    force: Boolean = false,
    auditData: Map[String, String] = Map.empty,
    eventUrl: Option[String] = None
  )

object EmailTemplateData {
  implicit val format: OFormat[EmailTemplateData] = Json.format[EmailTemplateData]
}

case class DraftEmail(
    emailUUID: String,
    templateData: EmailTemplateData,
    recipientTitle: String,
    userSelectionQuery: DevelopersEmailQuery,
    attachmentDetails: Option[Seq[UploadedFileWithObjectStore]],
    markdownEmailBody: String,
    htmlEmailBody: String,
    subject: String,
    status: EmailStatus,
    composedBy: String,
    approvedBy: Option[String],
    createDateTime: Instant,
    emailsCount: Int
  )

object DraftEmail {
  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit val format: OFormat[DraftEmail] = Json.format[DraftEmail]
}
