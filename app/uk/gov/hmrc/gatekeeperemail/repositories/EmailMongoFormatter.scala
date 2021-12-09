/*
 * Copyright 2021 HM Revenue & Customs
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

import org.joda.time.DateTime
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.gatekeeperemail.models.Email
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats

private[repositories] object EmailMongoFormatter {
  implicit val dateFormation  : Format[DateTime] = MongoJodaFormats.dateTimeFormat
  implicit val emailFormatter: OFormat[Email] = Json.format[Email]
}
