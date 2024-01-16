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

import play.api.libs.json.Format
import uk.gov.hmrc.apiplatform.modules.common.domain.services.SealedTraitJsonFormatting

sealed trait EmailStatus

object EmailStatus {

  case object FAILED  extends EmailStatus
  case object PENDING extends EmailStatus
  case object SENT    extends EmailStatus
  val values: Set[EmailStatus] = Set(FAILED, PENDING, SENT)

  def apply(text: String): Option[EmailStatus] = EmailStatus.values.find(_.toString() == text.toUpperCase)

  def unsafeApply(text: String): EmailStatus = apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid Email Status"))

  implicit val format: Format[EmailStatus] = SealedTraitJsonFormatting.createFormatFor[EmailStatus]("Email Status", apply)
}

sealed trait TopicOptionChoice

object TopicOptionChoice {

  case object BUSINESS_AND_POLICY extends TopicOptionChoice
  case object TECHNICAL           extends TopicOptionChoice
  case object RELEASE_SCHEDULES   extends TopicOptionChoice
  case object EVENT_INVITES       extends TopicOptionChoice
  val values = Set(BUSINESS_AND_POLICY, TECHNICAL, RELEASE_SCHEDULES, EVENT_INVITES)

  def apply(text: String): Option[TopicOptionChoice] = TopicOptionChoice.values.find(_.toString() == text.toUpperCase)

  def unsafeApply(text: String): TopicOptionChoice = apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid Topic Option Choice"))

  implicit val format: Format[TopicOptionChoice] = SealedTraitJsonFormatting.createFormatFor[TopicOptionChoice]("Topic Option Choice", apply)
}
