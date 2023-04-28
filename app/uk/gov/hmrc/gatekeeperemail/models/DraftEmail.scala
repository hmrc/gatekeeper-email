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

import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID
import scala.collection.immutable

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

case class EmailTemplateData(
    templateId: String,
    parameters: Map[String, String],
    force: Boolean = false,
    auditData: Map[String, String] = Map.empty,
    eventUrl: Option[String] = None
  )

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
    createDateTime: LocalDateTime,
    emailsCount: Int
  )

case class OutgoingEmail(
    emailUUID: String,
    recipientTitle: String,
    userSelectionQuery: DevelopersEmailQuery,
    attachmentDetails: Option[Seq[UploadedFileWithObjectStore]] = None,
    markdownEmailBody: String,
    htmlEmailBody: String,
    subject: String,
    status: EmailStatus,
    composedBy: String,
    approvedBy: Option[String],
    emailsCount: Int
  )

object OutgoingEmail {
  implicit val emailOverrideFormatter                                                       = Json.format[EmailOverride]
  implicit val developersEmailQueryFormatter: OFormat[DevelopersEmailQuery]                 = Json.format[DevelopersEmailQuery]
  implicit val format: OFormat[UploadCargo]                                                 = Json.format[UploadCargo]
  implicit val attachmentDetailsFormat: OFormat[UploadedFile]                               = Json.format[UploadedFile]
  implicit val attachmentDetailsWithObjectStoreFormat: OFormat[UploadedFileWithObjectStore] = Json.format[UploadedFileWithObjectStore]
  implicit val outGoingEmailFmt: OFormat[OutgoingEmail]                                     = Json.format[OutgoingEmail]
}

object DraftEmail {
  implicit val dateFormatter: Format[LocalDateTime]                                         = MongoJavatimeFormats.localDateTimeFormat
  implicit val emailOverrideFormatter                                                       = Json.format[EmailOverride]
  implicit val developersEmailQueryFormatter: OFormat[DevelopersEmailQuery]                 = Json.format[DevelopersEmailQuery]
  implicit val format: OFormat[UploadCargo]                                                 = Json.format[UploadCargo]
  implicit val attachmentDetailsFormat: OFormat[UploadedFile]                               = Json.format[UploadedFile]
  implicit val attachmentDetailsWithObjectStoreFormat: OFormat[UploadedFileWithObjectStore] = Json.format[UploadedFileWithObjectStore]
  implicit val emailTemplateDataFormatter: OFormat[EmailTemplateData]                       = Json.format[EmailTemplateData]
  implicit val emailFormatter: OFormat[DraftEmail]                                          = Json.format[DraftEmail]
}

case class SentEmail(
    updatedAt: LocalDateTime,
    emailUuid: UUID,
    firstName: String,
    lastName: String,
    recipient: String,
    status: EmailStatus,
    failedCount: Int,
    id: UUID = UUID.randomUUID(),
    createdAt: LocalDateTime = now()
  )

sealed abstract class EmailStatus(override val entryName: String) extends EnumEntry

object EmailStatus extends Enum[EmailStatus] with PlayJsonEnum[EmailStatus] {
  val values: immutable.IndexedSeq[EmailStatus] = findValues

  case object FAILED  extends EmailStatus("FAILED")
  case object PENDING extends EmailStatus("PENDING")
  case object SENT    extends EmailStatus("SENT")
}
case class EmailOverride(email: List[RegisteredUser], isOverride: Boolean = false)

case class DevelopersEmailQuery(
    topic: Option[String] = None,
    apis: Option[Seq[String]] = None,
    apiCategories: Option[Seq[APICategory]] = None,
    privateapimatch: Boolean = false,
    apiVersionFilter: Option[String] = None,
    allUsers: Boolean = false,
    emailsForSomeCases: Option[EmailOverride] = None
  )
