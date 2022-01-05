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

case class Email(recepientTitle: String, recepients: List[String], attachmentLink: Option[String], markdownEmailBody: String,
                 htmlEmailBody: String, subject: String, composedBy: String, approvedBy: Option[String], createDateTime: DateTime)

case class OutgoingEmail(recepientTitle: String, recepients: List[String], attachmentLink: Option[String],
                          markdownEmailBody: String, htmlEmailBody: String, subject: String,
                         composedBy: String, approvedBy: Option[String])

object OutgoingEmail {
  implicit val emailFmt: OFormat[OutgoingEmail] = Json.format[OutgoingEmail]
}