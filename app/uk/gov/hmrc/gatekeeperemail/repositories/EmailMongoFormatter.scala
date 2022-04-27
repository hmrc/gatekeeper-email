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

package uk.gov.hmrc.gatekeeperemail.repositories

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.gatekeeperemail.models.{DraftEmail, EmailStatus, EmailTemplateData, UploadCargo, UploadedFile, UploadedFileWithObjectStore, User}
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats

private[repositories] object EmailMongoFormatter {
  implicit val userFormatter: OFormat[User] = Json.format[User]
  implicit val cargoFormat: OFormat[UploadCargo] = Json.format[UploadCargo]
  implicit val attachmentDetailsFormat: OFormat[UploadedFile] = Json.format[UploadedFile]
  implicit val attachmentDetailsWithObjectStoreFormat: OFormat[UploadedFileWithObjectStore] = Json.format[UploadedFileWithObjectStore]
  implicit val emailTemplateDataFormatter: OFormat[EmailTemplateData] = Json.format[EmailTemplateData]
  implicit val emailFormatter: OFormat[DraftEmail] = Json.format[DraftEmail]
}
