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
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.gatekeeperemail.services.UpscanCallbackDispatcher
import uk.gov.hmrc.gatekeeperemail.util.HttpUrlFormat
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.gatekeeperemail.controllers.CallbackBody._
import uk.gov.hmrc.gatekeeperemail.models.Reference

import java.net.URL
import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

sealed trait CallbackBody {
  def reference : Reference
}

case class ReadyCallbackBody(
                              reference: Reference,
                              downloadUrl: URL,
                              uploadDetails: UploadDetails
                            ) extends CallbackBody

case class FailedCallbackBody(
                               reference: Reference,
                               failureDetails: ErrorDetails
                             ) extends CallbackBody

object CallbackBody {
  // must be in scope to create Reads for ReadyCallbackBody
  private implicit val urlFormat: Format[URL] = HttpUrlFormat.format

  implicit val uploadDetailsReads = Json.reads[UploadDetails]
  implicit val uploadDetailsWrites = Json.writes[UploadDetails]
  implicit val uploadDetails = Format(uploadDetailsReads, uploadDetailsWrites)

  implicit val errorDetailsReads = Json.reads[ErrorDetails]

  implicit val readyCallbackBodyReads = Json.reads[ReadyCallbackBody]

  implicit val failedCallbackBodyReads = Json.reads[FailedCallbackBody]

  implicit val reads = new Reads[CallbackBody] {
    override def reads(json: JsValue): JsResult[CallbackBody] = json \ "fileStatus" match {
      case JsDefined(JsString("READY")) => implicitly[Reads[ReadyCallbackBody]].reads(json)
      case JsDefined(JsString("FAILED")) => implicitly[Reads[FailedCallbackBody]].reads(json)
      case JsDefined(value) => JsError(s"Invalid type distriminator: $value")
      case JsUndefined() => JsError(s"Missing type distriminator")
    }
  }
}

case class UploadDetails(uploadTimestamp: Instant,
                         checksum: String,
                         fileMimeType: String,
                         fileName: String,
                         size: Long)

case class ErrorDetails(failureReason: String, message: String)


@Singleton
class UploadCallbackController @Inject()(upscanCallbackDispatcher : UpscanCallbackDispatcher,
                                         cc: ControllerComponents)
                                        (implicit ec : ExecutionContext) extends BackendController(cc) {

  private val logger = Logger(this.getClass)

  val callback = Action.async(parse.json) { implicit request =>
    logger.info(s"Received callback notification [${Json.stringify(request.body)}]")
    withJsonBody[CallbackBody] { feedback: CallbackBody =>
      upscanCallbackDispatcher.handleCallback(feedback).map(_ => Ok)
    }
  }
}