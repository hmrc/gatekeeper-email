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

package uk.gov.hmrc.gatekeeperemail.models.requests

import play.api.libs.json.{Json, OFormat}

case class EmailData(emailSubject: String, emailBody: String)

object EmailData {
  implicit val format: OFormat[EmailData] = Json.format[EmailData]
}

case class SendEmailRequest(
    to: String,
    templateId: String,
    parameters: Map[String, String],
    force: Boolean = false,
    auditData: Map[String, String] = Map.empty,
    eventUrl: Option[String] = None,
    tags: Map[String, String] = Map.empty
  )

object SendEmailRequest {
  implicit val format: OFormat[SendEmailRequest] = Json.format[SendEmailRequest]
}

case class DraftEmailRequest(
    userSelectionQuery: DevelopersEmailQuery,
    templateId: String,
    parameters: Map[String, String],
    force: Boolean = false,
    auditData: Map[String, String] = Map.empty,
    eventUrl: Option[String] = None
  )

object DraftEmailRequest {
  implicit val format: OFormat[DraftEmailRequest] = Json.format[DraftEmailRequest]
}

case class OneEmailRequest(
    to: List[String],
    templateId: String,
    parameters: Map[String, String],
    force: Boolean = false,
    auditData: Map[String, String] = Map.empty,
    eventUrl: Option[String] = None,
    tags: Map[String, String] = Map.empty
  )

object OneEmailRequest {
  implicit val format: OFormat[OneEmailRequest] = Json.format[OneEmailRequest]
}

case class EmailRequest(
    userSelectionQuery: DevelopersEmailQuery,
    templateId: String,
    emailData: EmailData,
    force: Boolean = false,
    auditData: Map[String, String] = Map.empty,
    eventUrl: Option[String] = None
  )

object EmailRequest {
  implicit val format: OFormat[EmailRequest] = Json.format[EmailRequest]
}

case class TestEmailRequest(email: String)

object TestEmailRequest {
  implicit val format: OFormat[TestEmailRequest] = Json.format[TestEmailRequest]
}
