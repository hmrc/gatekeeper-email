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

import java.time.{Instant, LocalDateTime, ZoneOffset}

import play.api.libs.json.Json

import uk.gov.hmrc.gatekeeperemail.models.{DraftEmail, SentEmail}

trait RawEmailTestData {
  def dateToJsonObj(date: Instant) = Json.obj(f"$$date" -> date.toEpochMilli)

  def toJsonObject(draftEmail: DraftEmail, createdDateTime: LocalDateTime, isUsingInstant: Boolean) = {
    val createDateTimeString: String   = createdDateTime.toString
    val createDateTimeInstant: Instant = createdDateTime.toInstant(ZoneOffset.UTC)

    Json.obj(
      "emailUUID"          -> draftEmail.emailUUID,
      "templateData"       -> draftEmail.templateData,
      "recipientTitle"     -> draftEmail.recipientTitle,
      "userSelectionQuery" -> draftEmail.userSelectionQuery,
      "attachmentDetails"  -> draftEmail.attachmentDetails,
      "markdownEmailBody"  -> draftEmail.markdownEmailBody,
      "htmlEmailBody"      -> draftEmail.htmlEmailBody,
      "subject"            -> draftEmail.subject,
      "status"             -> draftEmail.status,
      "composedBy"         -> draftEmail.composedBy,
      "approvedBy"         -> draftEmail.approvedBy,
      "createDateTime"     -> (if (isUsingInstant) createDateTimeInstant else createDateTimeString),
      "emailsCount"        -> draftEmail.emailsCount,
      "isUsingInstant"     -> isUsingInstant
    )
  }

  def toJsonObject(sentEmail: SentEmail, createdAt: LocalDateTime, updatedAt: LocalDateTime, isUsingInstant: Boolean) = {
    val createdAtString: String   = createdAt.toString
    val createdAtInstant: Instant = createdAt.toInstant(ZoneOffset.UTC)
    val updatedAtString: String   = updatedAt.toString
    val updatedAtInstant: Instant = updatedAt.toInstant(ZoneOffset.UTC)

    Json.obj(
      "updatedAt"      -> (if (isUsingInstant) updatedAtInstant else updatedAtString),
      "emailUuid"      -> sentEmail.emailUuid,
      "firstName"      -> sentEmail.firstName,
      "lastName"       -> sentEmail.lastName,
      "recipient"      -> sentEmail.recipient,
      "status"         -> sentEmail.status,
      "failedCount"    -> sentEmail.failedCount,
      "id"             -> sentEmail.id,
      "createdAt"      -> (if (isUsingInstant) createdAtInstant else createdAtString),
      "isUsingInstant" -> isUsingInstant
    )
  }

  def toJsonString(draftEmail: DraftEmail): String = {
    s"""{
       |  "emailUUID" : "${draftEmail.emailUUID}",
       |  "templateData" : {
       |    "templateId" : "${draftEmail.templateData.templateId}",
       |    "parameters" : { },
       |    "force" : ${draftEmail.templateData.force},
       |    "auditData" : { }
       |  },
       |  "recipientTitle" : "${draftEmail.recipientTitle}",
       |  "userSelectionQuery" : {
       |    "privateapimatch" : ${draftEmail.userSelectionQuery.privateapimatch},
       |    "allUsers" : ${draftEmail.userSelectionQuery.allUsers}
       |  },
       |  "attachmentDetails" : [ ],
       |  "markdownEmailBody" : "${draftEmail.markdownEmailBody}",
       |  "htmlEmailBody" : "${draftEmail.htmlEmailBody}",
       |  "subject" : "${draftEmail.subject}",
       |  "status" : "${draftEmail.status.entryName}",
       |  "composedBy" : "${draftEmail.composedBy}",
       |  "approvedBy" : "${draftEmail.approvedBy.get}",
       |  "createDateTime" : {
       |    "$$date" : {
       |      "$$numberLong" : "${draftEmail.createDateTime.toEpochMilli}"
       |    }
       |  },
       |  "emailsCount" : ${draftEmail.emailsCount}
       |}""".stripMargin
  }

  def toJsonString(sentEmail: SentEmail): String = {
    s"""{
       |  "updatedAt" : {
       |    "$$date" : {
       |      "$$numberLong" : "${sentEmail.updatedAt.toEpochMilli}"
       |    }
       |  },
       |  "emailUuid" : "${sentEmail.emailUuid.toString}",
       |  "firstName" : "${sentEmail.firstName}",
       |  "lastName" : "${sentEmail.lastName}",
       |  "recipient" : "${sentEmail.recipient}",
       |  "status" : "${sentEmail.status.entryName}",
       |  "failedCount" : ${sentEmail.failedCount},
       |  "id" : "${sentEmail.id.toString}",
       |  "createdAt" : {
       |    "$$date" : {
       |      "$$numberLong" : "${sentEmail.createdAt.toEpochMilli}"
       |    }
       |  }
       |}""".stripMargin
  }
}

object RawEmailTestData extends RawEmailTestData {}
