/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.libs.json.{Format, JsValue, Json, Writes}
import play.api.mvc._
import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.models.Reference
import uk.gov.hmrc.gatekeeperemail.services.{FileUploadStatusService, UploadProgressTracker}
import uk.gov.hmrc.gatekeeperemail.util.ApplicationLogger
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.gatekeeperemail.models.JsonFormatters._
import uk.gov.hmrc.gatekeeperemail.models.{UploadId, UploadStatus}

import java.util.UUID.randomUUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class UploadFormController @Inject()(
                                      uploadProgressTracker: FileUploadStatusService,
                                      cc: ControllerComponents,
                                      playBodyParsers: PlayBodyParsers)
                                    ( implicit ec: ExecutionContext
                                    ) extends BackendController(cc) with ApplicationLogger {


  private def handleOption[T](future: Future[Option[T]])(implicit writes: Writes[T]): Future[Result] = {
    future.map {
      case Some(v) => Ok(toJson(v))
      case None => BadRequest("No uploadInfo found")
    }
  }

  private def handleFuture[T](future: Future[T])(implicit writes: Writes[T]): Future[Result] = {
    future map { v =>
      Ok(toJson(v))
    }
  }


  def addUploadedFileStatus(key: String, emailUUID: String) : Action[AnyContent] = Action.async {
    logger.info(s"Got a insert request for key: $key")
    val result = uploadProgressTracker.requestUpload(key, emailUUID)
    handleFuture(result)
  }

  def updateUploadedFileStatus(reference: String) : Action[JsValue] = Action.async(playBodyParsers.json) { implicit request =>
    withJsonBody[UploadStatus] { uploadStatus =>
      val result = uploadProgressTracker.registerUploadResult(reference, uploadStatus)
      handleFuture(result)
    }
  }

  def fetchUploadedFileStatus(key: String) : Action[AnyContent] = Action.async {
    logger.info(s"Got a fetch request for key: $key")
    val result = for (uploadResult <- uploadProgressTracker.getUploadResult(key)) yield uploadResult
    handleOption(result)
  }
}
