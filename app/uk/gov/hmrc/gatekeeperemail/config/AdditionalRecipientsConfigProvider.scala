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

package uk.gov.hmrc.gatekeeperemail.config

import scala.jdk.CollectionConverters.CollectionHasAsScala

import play.api.ConfigLoader
import play.api.libs.json.{Json, OFormat}

import uk.gov.hmrc.gatekeeperemail.models.EmailRecipient

case class AdditionalRecipient(email: String, firstName: String, lastName: String) extends EmailRecipient

object AdditionalRecipient {
  implicit val format: OFormat[AdditionalRecipient] = Json.format[AdditionalRecipient]
}

object AdditionalRecipientsConfigProvider {

  implicit val configLoader: ConfigLoader[List[AdditionalRecipient]] = ConfigLoader(_.getStringList).map(
    _.asScala.toList.map(_.split(','))
      .filter(_.length == 3)
      .map(userDetails => AdditionalRecipient(userDetails(0), userDetails(1), userDetails(2)))
  )
}
