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

import java.time.Instant

import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import uk.gov.hmrc.gatekeeperemail.models.requests.DevelopersEmailQuery
import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.time.LocalDateTime
import java.time.ZoneOffset

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

object DraftEmail extends EnvReads {
  private val readCreateDateTimeAsInstant = (JsPath \ "createDateTime").read[Instant](MongoJavatimeFormats.instantFormat)
  private val readCreateDateTimeAsLDT     = (JsPath \ "createDateTime").read[LocalDateTime](DefaultLocalDateTimeReads).map(ldt => ldt.toInstant(ZoneOffset.UTC))

  implicit val writes: OWrites[DraftEmail]    = Json.writes[DraftEmail]
  implicit val reads: Reads[DraftEmail] = (
    (JsPath \ "emailUUID").read[String] and
    (JsPath \ "templateData").read[EmailTemplateData] and
    (JsPath \ "recipientTitle").read[String] and
    (JsPath \ "userSelectionQuery").read[DevelopersEmailQuery] and
    (JsPath \ "attachmentDetails").readNullable[Seq[UploadedFileWithObjectStore]] and
    (JsPath \ "markdownEmailBody").read[String] and
    (JsPath \ "htmlEmailBody").read[String] and
    (JsPath \ "subject").read[String] and
    (JsPath \ "status").read[EmailStatus] and
    (JsPath \ "composedBy").read[String] and
    (JsPath \ "approvedBy").readNullable[String] and
    (readCreateDateTimeAsInstant.orElse(readCreateDateTimeAsLDT)) and
    (JsPath \ "emailsCount").read[Int]
  )(DraftEmail.apply _)
  
  implicit val format: OFormat[DraftEmail]    = OFormat(reads, writes)
}