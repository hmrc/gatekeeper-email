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

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.http.Writeable
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.Json.toJson
import play.api.libs.json.Writes
import play.api.mvc._
import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.connectors.{Reference, UpscanInitiateConnector}
import uk.gov.hmrc.gatekeeperemail.controllers.routes.UploadFormController
import uk.gov.hmrc.gatekeeperemail.model.{UploadId, UploadedFailedWithErrors, UploadedSuccessfully}
import uk.gov.hmrc.gatekeeperemail.services.UploadProgressTracker
import uk.gov.hmrc.gatekeeperemail.util.ApplicationLogger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.gatekeeperemail.models.JsonFormatters._
import uk.gov.hmrc.gatekeeperemail.repository.UploadDetails._
import uk.gov.hmrc.upscan.services.ErrorRedirect

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadFormController @Inject()(upscanInitiateConnector: UpscanInitiateConnector,
                                     uploadProgressTracker: UploadProgressTracker,
                                     cc: ControllerComponents)
                                    (implicit appConfig: AppConfig,
                                     ec: ExecutionContext,
                                     hc: HeaderCarrier) extends BackendController(cc) with ApplicationLogger {

  private def handleFuture[T](future: Future[T])(implicit writes: Writes[T]): Future[Result] = {
    future.map {
      v => Ok(toJson(v))
    }
  }

  private def handleOption[T](future: Future[Option[T]])(implicit writes: Writes[T]): Future[Result] = {
    future.map {
      case Some(v) => Ok(toJson(v))
      case None => BadRequest("No application was found")
    }
  }

  def showV2(): Action[AnyContent] = Action.async {
    val uploadId           = UploadId.generate
    val successRedirectUrl = appConfig.uploadRedirectTargetBase + "/hello-world-upscan/hello-world/success"
    val errorRedirectUrl   = appConfig.uploadRedirectTargetBase + "/hello-world-upscan/hello-world/error"
    val result = for {
      upscanInitiateResponse <- upscanInitiateConnector.initiateV2(Some(successRedirectUrl), Some(errorRedirectUrl))
      _                      <- uploadProgressTracker.requestUpload(uploadId, Reference(upscanInitiateResponse.fileReference.reference))
    } yield upscanInitiateResponse
    handleFuture(result)
  }

  def showResult(uploadId: UploadId): Action[AnyContent] = Action.async { implicit request =>
    val result = for (uploadResult <- uploadProgressTracker.getUploadResult(uploadId)) yield uploadResult
    handleOption(result)
  }

  def addUploadedFileStatus(uploadId: UploadId, reference: Reference) : Action[AnyContent] = Action.async { implicit request =>
    for (uploadResult <- uploadProgressTracker.requestUpload(uploadId, reference)) yield uploadResult
    Future.successful(Ok(s"File with uploadId: ${uploadId}, reference: ${reference} is inserted with status: InProgress"))
  }

  def fetchUploadedFileStatus(uploadId: UploadId) : Action[AnyContent] = Action.async { implicit request =>
    val result = for (uploadResult <- uploadProgressTracker.getUploadResult(uploadId)) yield uploadResult
    handleOption(result)
  }

  def showError(errorCode: String, errorMessage: String, errorRequestId: String, key: String): Action[AnyContent] = Action.async { implicit request =>
      uploadProgressTracker.registerUploadResult(Reference(key), UploadedFailedWithErrors(errorCode, errorMessage,errorRequestId, key))
    Future.successful(Ok(s"Captured Errors $errorCode"))
  }

  private case class SampleForm(field1: String, field2: String, uploadedFileId: UploadId)

  private val sampleForm = Form(
    mapping(
      "field1"         -> text,
      "field2"         -> text,
      "uploadedFileId" -> text.transform[UploadId](UploadId(_), _.value)
    )(SampleForm.apply)(SampleForm.unapply)
  )

  def showSubmissionForm(uploadId: UploadId): Action[AnyContent] = Action.async { implicit request =>
    val emptyForm = sampleForm.fill(SampleForm("", "", uploadId))
    val result = for (uploadResult <- uploadProgressTracker.getUploadResult(uploadId)) yield uploadResult
    handleOption(result)
  }

  def submitFormWithFile(): Action[AnyContent] = Action.async { implicit request =>
    sampleForm
      .bindFromRequest()
      .fold(
        errors => {
          Future.successful(BadRequest(s"Problem with a form $errors"))
        },
        _ => {
          logger.info("Form successfully submitted")
          Future.successful(Ok)
        }
      )
  }

  def showSubmissionResult(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok)
  }
}
