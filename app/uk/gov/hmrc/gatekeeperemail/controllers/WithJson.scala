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

package uk.gov.hmrc.gatekeeperemail.controllers

import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue, Reads}
import play.api.mvc.Results.BadRequest
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.gatekeeperemail.models.{ErrorCode, JsErrorResponse}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait WithJson {
  val logger: Logger = Logger(getClass.getName)

  def withJson[T](f: (T) => Future[Result])(implicit request: Request[JsValue], m: Manifest[T], reads: Reads[T]): Future[Result] =
  Try(request.body.validate[T]) match {
    case Success(JsSuccess(payload, _)) => f(payload)
    case Success(JsError(errs)) =>
      logger.info(s"Invalid ${m.runtimeClass.getSimpleName} payload: $errs")
      Future.successful(BadRequest(JsErrorResponse(ErrorCode.INVALID_REQUEST_PAYLOAD, "JSON body is invalid against expected format")))
    case Failure(e) =>
      logger.info(s"Could not parse body due to ${e.getMessage}")
      Future.successful(BadRequest(JsErrorResponse(ErrorCode.INVALID_REQUEST_PAYLOAD, e.getMessage)))
  }
}
