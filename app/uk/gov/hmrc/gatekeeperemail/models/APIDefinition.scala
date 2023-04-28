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

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

import play.api.libs.json.Json

sealed trait APIAccessType extends EnumEntry

object APIAccessType extends Enum[APIAccessType] with PlayJsonEnum[APIAccessType] {
  val values = findValues
  case object PRIVATE extends APIAccessType
  case object PUBLIC  extends APIAccessType
}

case class APICategory(value: String) extends AnyVal

object APICategory {
  implicit val formatApiCategory = Json.valueFormat[APICategory]
}
