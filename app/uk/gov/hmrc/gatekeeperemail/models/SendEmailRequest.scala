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

import play.api.libs.json.{Json, OFormat}

case class EmailData(emailSubject: String, emailBody: String)

case class SendEmailRequest(to: String,
                            templateId: String,
                            parameters: Map[String, String],
                            force: Boolean = false,
                            auditData: Map[String, String] = Map.empty,
                            eventUrl: Option[String] = None,
                            tags: Map[String, String] = Map.empty)

case class DraftEmailRequest(to: List[User],
                            templateId: String,
                            parameters: Map[String, String],
                            force: Boolean = false,
                            auditData: Map[String, String] = Map.empty,
                            eventUrl: Option[String] = None)

case class OneEmailRequest(to: List[String],
                            templateId: String,
                            parameters: Map[String, String],
                            force: Boolean = false,
                            auditData: Map[String, String] = Map.empty,
                            eventUrl: Option[String] = None,
                           tags: Map[String, String] = Map.empty)

case class EmailRequest(to: List[User],
                        templateId: String,
                        emailData: EmailData,
                        force: Boolean = false,
                        auditData: Map[String, String] = Map.empty,
                        eventUrl: Option[String] = None,
                        attachmentDetails: Option[Seq[UploadedFileWithObjectStore]] = None)

case class EmailSaved(emailUUID: String)

object SendEmailRequest {
  implicit val userFmt: OFormat[User] = Json.format[User]
  implicit val format: OFormat[UploadCargo] = Json.format[UploadCargo]
  implicit val attachmentDetailsFormat: OFormat[UploadedFile] = Json.format[UploadedFile]
  implicit val attachmentDetailsWithObjectStoreFormat: OFormat[UploadedFileWithObjectStore] = Json.format[UploadedFileWithObjectStore]
  implicit val sendEmailRequestFmt: OFormat[SendEmailRequest] = Json.format[SendEmailRequest]
}

object OneEmailRequest {
  implicit val userFmt: OFormat[User] = Json.format[User]
  implicit val format: OFormat[UploadCargo] = Json.format[UploadCargo]
  implicit val attachmentDetailsFormat: OFormat[UploadedFile] = Json.format[UploadedFile]
  implicit val attachmentDetailsWithObjectStoreFormat: OFormat[UploadedFileWithObjectStore] = Json.format[UploadedFileWithObjectStore]
  implicit val sendEmailRequestFmt: OFormat[OneEmailRequest] = Json.format[OneEmailRequest]
}

object EmailRequest {
  implicit val userFmt: OFormat[User] = Json.format[User]
  implicit val format: OFormat[UploadCargo] = Json.format[UploadCargo]
  implicit val attachmentDetailsFormat: OFormat[UploadedFile] = Json.format[UploadedFile]
  implicit val attachmentDetailsWithObjectStoreFormat: OFormat[UploadedFileWithObjectStore] = Json.format[UploadedFileWithObjectStore]
  implicit val receiveEmailRequestFmt: OFormat[EmailRequest] = Json.format[EmailRequest]
}

object EmailData {
  implicit val userFmt: OFormat[User] = Json.format[User]
  implicit val format: OFormat[UploadCargo] = Json.format[UploadCargo]
  implicit val attachmentDetailsFormat: OFormat[UploadedFile] = Json.format[UploadedFile]
  implicit val attachmentDetailsWithObjectStoreFormat: OFormat[UploadedFileWithObjectStore] = Json.format[UploadedFileWithObjectStore]
  implicit val emailDataFmt: OFormat[EmailData] = Json.format[EmailData]
}