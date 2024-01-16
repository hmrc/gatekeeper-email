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

package uk.gov.hmrc.gatekeeperemail.models.responses

import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.{JsObject, Json}

sealed trait ErrorCode

object ErrorCode {
  case object ACCEPT_HEADER_INVALID   extends ErrorCode
  case object INVALID_REQUEST_PAYLOAD extends ErrorCode
  case object INTERNAL_SERVER_ERROR   extends ErrorCode
  case object BAD_REQUEST             extends ErrorCode

  val values = Set(ACCEPT_HEADER_INVALID, INVALID_REQUEST_PAYLOAD, INTERNAL_SERVER_ERROR, BAD_REQUEST)

  def apply(text: String): Option[ErrorCode] = ErrorCode.values.find(_.toString() == text.toUpperCase)

  def unsafeApply(text: String): ErrorCode = apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid Error Code"))
}

object JsErrorResponse {

  def apply(errorCode: ErrorCode, message: JsValueWrapper): JsObject =
    Json.obj(
      "code"    -> errorCode.toString,
      "message" -> message
    )
}
