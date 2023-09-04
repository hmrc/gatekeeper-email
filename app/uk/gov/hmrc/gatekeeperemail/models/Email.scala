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

import scala.collection.immutable

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

import play.api.libs.json.{Format, Json}

import uk.gov.hmrc.gatekeeperemail.models

sealed abstract class EmailStatus(override val entryName: String) extends EnumEntry

object EmailStatus extends Enum[EmailStatus] with PlayJsonEnum[EmailStatus] {
  val values: immutable.IndexedSeq[EmailStatus] = findValues

  case object FAILED  extends EmailStatus("FAILED")
  case object PENDING extends EmailStatus("PENDING")
  case object SENT    extends EmailStatus("SENT")
}

object TopicOptionChoice extends Enumeration {
  type TopicOptionChoice = Value

  val BUSINESS_AND_POLICY, TECHNICAL, RELEASE_SCHEDULES, EVENT_INVITES = Value

  implicit val format: Format[models.TopicOptionChoice.Value] = Json.formatEnum(TopicOptionChoice)

}
