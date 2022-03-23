/*
 * Copyright 2022 HM Revenue & Customs
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

import org.joda.time.DateTime
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.gatekeeperemail.models.EmailStatus.Status
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats

case class EmailTemplateData(templateId: String, parameters: Map[String, String],
                             force: Boolean = false,
                             auditData: Map[String, String] = Map.empty,
                             eventUrl: Option[String] = None)

case class Email(emailUUID: String, templateData: EmailTemplateData, recipientTitle: String, recipients: List[User],
                 attachmentDetails: Option[Seq[UploadedFileWithObjectStore]], markdownEmailBody: String,
                 htmlEmailBody: String, subject: String, status: String, composedBy: String, approvedBy: Option[String], createDateTime: DateTime)

case class OutgoingEmail(emailUUID: String, recipientTitle: String, recipients: List[User], attachmentDetails: Option[Seq[UploadedFileWithObjectStore]] = None,
                         markdownEmailBody: String, htmlEmailBody: String, subject: String, status: String,
                         composedBy: String, approvedBy: Option[String])

object OutgoingEmail {
  implicit val userFormatter: OFormat[User] = Json.format[User]
  implicit val format: OFormat[UploadCargo] = Json.format[UploadCargo]
  implicit val attachmentDetailsFormat: OFormat[UploadedFile] = Json.format[UploadedFile]
  implicit val attachmentDetailsWithObjectStoreFormat: OFormat[UploadedFileWithObjectStore] = Json.format[UploadedFileWithObjectStore]
  implicit val outGoingEmailFmt: OFormat[OutgoingEmail] = Json.format[OutgoingEmail]
}

object Email {
  implicit val dateFormation: Format[DateTime] = MongoJodaFormats.dateTimeFormat
  implicit val userFormatter: OFormat[User] = Json.format[User]
  implicit val format: OFormat[UploadCargo] = Json.format[UploadCargo]
  implicit val attachmentDetailsFormat: OFormat[UploadedFile] = Json.format[UploadedFile]
  implicit val attachmentDetailsWithObjectStoreFormat: OFormat[UploadedFileWithObjectStore] = Json.format[UploadedFileWithObjectStore]
  implicit val emailTemplateDataFormatter: OFormat[EmailTemplateData] = Json.format[EmailTemplateData]
  implicit val emailFormatter: OFormat[Email] = Json.format[Email]
}

object EmailStatus extends Enumeration {
  type Status = Value
  val INPROGRESS, SENT = Value

  val displayedStatus: (Status) => String = {
    case EmailStatus.INPROGRESS => "INPROGRESS"
    case EmailStatus.SENT => "SENT"
  }
}