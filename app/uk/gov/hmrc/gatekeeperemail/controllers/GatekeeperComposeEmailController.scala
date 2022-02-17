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

import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, PlayBodyParsers, Result}
import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.models._
import uk.gov.hmrc.gatekeeperemail.services.EmailService
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient
import uk.gov.hmrc.objectstore.client.{ObjectSummaryWithMd5, Path, RetentionPeriod}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.io.IOException
import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GatekeeperComposeEmailController @Inject()(
  mcc: MessagesControllerComponents,
  playBodyParsers: PlayBodyParsers,
  emailService: EmailService,
  objectStoreClient: PlayObjectStoreClient,
  appConfig: AppConfig
  )(implicit val ec: ExecutionContext)
    extends BackendController(mcc) with WithJson {

  def saveEmail(emailUID: String): Action[JsValue] = Action.async(playBodyParsers.json) { implicit request =>
    withJson[EmailRequest] { receiveEmailRequest =>
      emailService.persistEmail(receiveEmailRequest, emailUID)
        .map(email => Ok(toJson(outgoingEmail(email))))
        .recover(recovery)
    }
  }

  def updateEmail(emailUID: String): Action[JsValue] = Action.async(playBodyParsers.json) { implicit request =>
    withJson[EmailRequest] { receiveEmailRequest =>
      emailService.updateEmail(receiveEmailRequest, emailUID)
        .map(email => Ok(toJson(outgoingEmail(email))))
        .recover(recovery)
    }
  }

  def updateFiles(): Action[UploadedFileMetadata] = Action.async(parse.json[UploadedFileMetadata]) { implicit request =>

    def uploadToObjectStore(downloadUrl: String, fileName: String) = {
      logger.info(s"uploadToObjectStore downloadUrl = $downloadUrl and fileName = $fileName")
      objectStoreClient.uploadFromUrl(from = new URL(downloadUrl),
        to = Path.File(Path.Directory("gatekeeper-email"), fileName),
        retentionPeriod = RetentionPeriod.parse(appConfig.defaultRetentionPeriod).getOrElse(RetentionPeriod.OneYear),
        contentType = None,
        contentMd5 = None,
        owner = "gatekeeper-email"
      )
    }
    request.body match {
      case value =>  {
        println(s"Cargo: ${value.cargo}")
        println(s"Callback received UploadedFileMetadata: $value")
        val fetchEmail: Future[Email] = emailService.fetchEmail(emailUID = value.cargo.get.emailUID)
        fetchEmail.map { email =>
          val filesToUploadInObjStore: Seq[UploadedFileWithObjectStore] =
            filesToUploadInObjectStore(email.attachmentDetails, value.uploadedFiles)
          println(s"filesToUploadInObjStore: $filesToUploadInObjStore")
          var latestUploadedFiles: Seq[UploadedFileWithObjectStore] = Seq.empty[UploadedFileWithObjectStore]
          filesToUploadInObjStore.foreach(uploadedFile => {
            val objectSummary: Future[ObjectSummaryWithMd5] = uploadToObjectStore(uploadedFile.downloadUrl, uploadedFile.fileName)
            objectSummary.map { summary =>
              println(s"*****latestUploadedFiles BEFORE APPENDING***: $latestUploadedFiles")
              latestUploadedFiles = latestUploadedFiles :+ uploadedFile.copy(objectStoreUrl = Some(summary.location.asUri))
              println(s"*****latestUploadedFiles AFTER APPENDING***: $latestUploadedFiles")
              println(s"latestUploadedFiles: $latestUploadedFiles")
              val finalUploadedFiles = email.attachmentDetails match {
                case Some(currentFiles) =>
                  println(s"*****currentFiles****: $currentFiles")
                  val finalList = currentFiles ++ latestUploadedFiles
                  println(s"*****currentFiles AFTER APPENDING****: $finalList")
                  finalList

                case None => latestUploadedFiles
              }
              println(s"*****finalUploadedFiles*******: $finalUploadedFiles")
              val er = EmailRequest(
                email.recipients,
                templateId = "gatekeeper",
                EmailData(email.subject, email.markdownEmailBody),
                attachmentDetails = Some(finalUploadedFiles))
              emailService.updateEmail(er, email.emailUID)
            }
          })
          NoContent
        }
      }
      case _ => Future.successful(BadRequest)
    }
  }

  def filesToUploadInObjectStore(existingFiles: Option[Seq[UploadedFileWithObjectStore]],
                                uploadedFiles: Seq[UploadedFileWithObjectStore]):
  Seq[UploadedFileWithObjectStore] = {
    existingFiles match {
      case Some(current) =>
          val intersection = current.intersect(uploadedFiles)
          if (intersection.size == uploadedFiles.size) {
            List()
          } else {
            uploadedFiles.diff(current)
          }
      case None => uploadedFiles
    }
  }

  def filesToRemoveFromObjectStore(existingFiles: Option[Seq[UploadedFileWithObjectStore]],
                                 uploadedFiles: Seq[UploadedFileWithObjectStore]):
  Seq[UploadedFileWithObjectStore] = {
    existingFiles match {
      case Some(current) =>
        val intersection = current.intersect(uploadedFiles)
        if (intersection.size == uploadedFiles.size) {
          List()
        } else {
          current.diff(uploadedFiles)
        }
      case None => uploadedFiles
    }
  }

  def fetchEmail(emailUID: String): Action[AnyContent] = Action.async { implicit request =>
      logger.info(s"In fetchEmail for $emailUID")
      emailService.fetchEmail(emailUID)
        .map(email => Ok(toJson(outgoingEmail(email))))
        .recover(recovery)
  }

  def sendEmail(emailUID: String): Action[AnyContent] = Action.async{ implicit request =>
    emailService.sendEmail(emailUID)
      .map(email => Ok(toJson(outgoingEmail(email))))
      .recover(recovery)
  }

  private def outgoingEmail(email: Email): OutgoingEmail = {
    OutgoingEmail(email.emailUID, email.recipientTitle, email.recipients, email.attachmentDetails,
      email.markdownEmailBody, email.htmlEmailBody, email.subject,
      email.composedBy, email.approvedBy)
  }

  private def recovery: PartialFunction[Throwable, Result] = {
    case e: IOException =>
      logger.warn(s"IOException ${e.getMessage}")
      InternalServerError(JsErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage))
  }
}
