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

import play.api.libs.json.{Json, OFormat}

import uk.gov.hmrc.gatekeeperemail.models._

private[repositories] object EmailMongoFormatter {
  implicit val cargoFormat: OFormat[UploadCargo]                                            = Json.format[UploadCargo]
  implicit val emailOverrideFormatter                                                       = Json.format[EmailOverride]
  implicit val developersEmailQueryFormatter: OFormat[DevelopersEmailQuery]                 = Json.format[DevelopersEmailQuery]
  implicit val attachmentDetailsFormat: OFormat[UploadedFile]                               = Json.format[UploadedFile]
  implicit val attachmentDetailsWithObjectStoreFormat: OFormat[UploadedFileWithObjectStore] = Json.format[UploadedFileWithObjectStore]
  implicit val emailTemplateDataFormatter: OFormat[EmailTemplateData]                       = Json.format[EmailTemplateData]
  implicit val draftEmailFormatter: OFormat[DraftEmail]                                     = Json.format[DraftEmail]
  implicit val sentEmailFormatter: OFormat[SentEmail]                                       = Json.format[SentEmail]
}
