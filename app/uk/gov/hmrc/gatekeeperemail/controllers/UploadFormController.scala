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

import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsValue, Writes}
import play.api.mvc._
import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.connectors.{Reference, UpscanInitiateConnector}
import uk.gov.hmrc.gatekeeperemail.model.{UploadId, UploadStatus}
import uk.gov.hmrc.gatekeeperemail.services.UploadProgressTracker
import uk.gov.hmrc.gatekeeperemail.util.ApplicationLogger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.gatekeeperemail.repository.UploadDetails._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadFormController @Inject()(
                                     uploadProgressTracker: UploadProgressTracker,
                                     cc: ControllerComponents)
                                    (implicit appConfig: AppConfig,
                                     ec: ExecutionContext,
                                     hc: HeaderCarrier) extends BackendController(cc) with ApplicationLogger {

  private def handleOption[T](future: Future[Option[T]])(implicit writes: Writes[T]): Future[Result] = {
    future.map {
      case Some(v) => Ok(toJson(v))
      case None => BadRequest("No uploadStatus found")
    }
  }

  def addUploadedFileStatus(uploadId: UploadId, reference: Reference) : Action[AnyContent] = Action.async { implicit request =>
    for (uploadResult <- uploadProgressTracker.requestUpload(uploadId, reference)) yield uploadResult
    Future.successful(Ok(s"File with uploadId: ${uploadId}, reference: ${reference} is inserted with status: InProgress"))
  }

  def updateUploadedFileStatus(uploadId: UploadId, reference: Reference) : Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[UploadStatus] { uploadStatus =>
      uploadProgressTracker.registerUploadResult(reference, uploadStatus)
      Future.successful(Ok(s"File with uploadId: ${uploadId}, reference: ${reference} is inserted with status: ${uploadStatus}"))
    }
  }

  def fetchUploadedFileStatus(uploadId: UploadId) : Action[AnyContent] = Action.async { implicit request =>
    val result = for (uploadResult <- uploadProgressTracker.getUploadResult(uploadId)) yield uploadResult
    handleOption(result)
  }
}
