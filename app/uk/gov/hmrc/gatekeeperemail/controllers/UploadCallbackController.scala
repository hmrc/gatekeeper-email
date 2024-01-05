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

package uk.gov.hmrc.gatekeeperemail.controllers

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.Logger
import play.api.libs.json.Json.toJson
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import uk.gov.hmrc.gatekeeperemail.controllers.CallbackBody._
import uk.gov.hmrc.gatekeeperemail.models.JsonFormatters._
import uk.gov.hmrc.gatekeeperemail.services.UpscanCallbackService

trait CallbackBody {
  def reference: String
}

case class ReadyCallbackBody(
    reference: String,
    downloadUrl: String,
    uploadDetails: UploadDetails
  ) extends CallbackBody

case class FailedCallbackBody(
    reference: String,
    fileStatus: String,
    failureDetails: ErrorDetails
  ) extends CallbackBody

object CallbackBody {
  implicit val uploadDetailsReads: Reads[UploadDetails]    = Json.reads[UploadDetails]
  implicit val uploadDetailsWrites: OWrites[UploadDetails] = Json.writes[UploadDetails]
  implicit val uploadDetails: Format[UploadDetails]        = Format(uploadDetailsReads, uploadDetailsWrites)

  implicit val errorDetailsReads: Reads[ErrorDetails] = Json.reads[ErrorDetails]

  implicit val readyCallbackBodyReads: Reads[ReadyCallbackBody] = Json.reads[ReadyCallbackBody]

  implicit val failedCallbackBodyReads: Reads[FailedCallbackBody] = Json.reads[FailedCallbackBody]

  implicit val reads: Reads[CallbackBody] = new Reads[CallbackBody] {

    override def reads(json: JsValue): JsResult[CallbackBody] = json \ "fileStatus" match {
      case JsDefined(JsString("READY"))  => implicitly[Reads[ReadyCallbackBody]].reads(json)
      case JsDefined(JsString("FAILED")) => implicitly[Reads[FailedCallbackBody]].reads(json)
      case JsDefined(value)              => JsError(s"Invalid file upload status type: $value")
      case JsUndefined()                 => JsError("Missing file upload status type")
    }
  }
}

case class UploadDetails(uploadTimestamp: Instant, checksum: String, fileMimeType: String, fileName: String, size: Long)

case class ErrorDetails(failureReason: String, message: String)

@Singleton
class UploadCallbackController @Inject() (upscanCallbackDispatcher: UpscanCallbackService, cc: ControllerComponents)(implicit ec: ExecutionContext) extends BackendController(cc) {

  private val logger = Logger(this.getClass)

  private def handleFuture[T](future: Future[T])(implicit writes: Writes[T]): Future[Result] = {
    future map { v =>
      Ok(toJson(v))
    }
  }

  val callback = Action.async(parse.json) { implicit request =>
    logger.info(s"Received callback notification [${Json.stringify(request.body)}]")
    withJsonBody[CallbackBody] { feedback: CallbackBody =>
      handleFuture(upscanCallbackDispatcher.handleCallback(feedback))
    }
  }
}
