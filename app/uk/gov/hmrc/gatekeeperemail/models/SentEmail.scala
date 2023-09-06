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

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.UUID

import play.api.libs.functional.syntax._
import play.api.libs.json.{EnvReads, JsPath, OWrites, Reads, _}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

case class SentEmail(
    updatedAt: Instant,
    emailUuid: UUID,
    firstName: String,
    lastName: String,
    recipient: String,
    status: EmailStatus,
    failedCount: Int,
    id: UUID = UUID.randomUUID(),
    createdAt: Instant,
    isUsingInstant: Option[Boolean] = None
  )

object SentEmail extends EnvReads {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private val readCreatedAtAsInstant = (JsPath \ "createdAt").read[Instant](MongoJavatimeFormats.instantFormat)
  private val readCreatedAtAsLDT     = (JsPath \ "createdAt").read[LocalDateTime](DefaultLocalDateTimeReads).map(ldt => ldt.toInstant(ZoneOffset.UTC))

  private val readUpdatedAtAsInstant = (JsPath \ "updatedAt").read[Instant](MongoJavatimeFormats.instantFormat)
  private val readUpdatedAtAsLDT     = (JsPath \ "updatedAt").read[LocalDateTime](DefaultLocalDateTimeReads).map(ldt => ldt.toInstant(ZoneOffset.UTC))

  implicit val writes: OWrites[SentEmail] = Json.writes[SentEmail]

  implicit val reads: Reads[SentEmail] = (
    (readUpdatedAtAsInstant.orElse(readUpdatedAtAsLDT)) and
      (JsPath \ "emailUuid").read[UUID] and
      (JsPath \ "firstName").read[String] and
      (JsPath \ "lastName").read[String] and
      (JsPath \ "recipient").read[String] and
      (JsPath \ "status").read[EmailStatus] and
      (JsPath \ "failedCount").read[Int] and
      (JsPath \ "id").read[UUID] and
      (readCreatedAtAsInstant.orElse(readCreatedAtAsLDT)) and
      (JsPath \ "isUsingInstant").readNullable[Boolean]
  )(SentEmail.apply _)

  implicit val format: OFormat[SentEmail] = OFormat(reads, writes)
}
