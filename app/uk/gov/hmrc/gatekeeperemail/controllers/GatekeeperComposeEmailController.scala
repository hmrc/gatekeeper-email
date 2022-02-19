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
import uk.gov.hmrc.gatekeeperemail.models.{UploadedFileWithObjectStore, _}
import uk.gov.hmrc.gatekeeperemail.services.{EmailService, ObjectStoreService}
import uk.gov.hmrc.http.HeaderCarrier
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
  objectStoreService: ObjectStoreService
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

  def updateFiles(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[UploadedFileMetadata] { value: UploadedFileMetadata =>
    val fetchEmail: Future[Email] = emailService.fetchEmail(emailUID = value.cargo.get.emailUID)
        fetchEmail.map { email =>
          val filesToUploadInObjStore: Seq[UploadedFileWithObjectStore] =
            filesToUploadInObjectStore(email.attachmentDetails, value.uploadedFiles)
          uploadFilesToObjectStoreAndUpdateEmailRecord(email, filesToUploadInObjStore)
          val filesToDeleteFromObjStore: Seq[UploadedFileWithObjectStore] =
            filesToRemoveFromObjectStore(email.attachmentDetails, value.uploadedFiles)
          deleteFilesFromObjectStoreAndUpdateEmailRecord(email, filesToDeleteFromObjStore)
          NoContent
        }
    }.recover(recovery)
  }

  def uploadFilesToObjectStoreAndUpdateEmailRecord(email: Email, filesToUploadInObjStore: Seq[UploadedFileWithObjectStore]) = {
    var latestUploadedFiles: Seq[UploadedFileWithObjectStore] = Seq.empty[UploadedFileWithObjectStore]
    filesToUploadInObjStore.foreach(uploadedFile => {
      val objectSummary: Future[ObjectSummaryWithMd5] = objectStoreService.uploadToObjectStore(email.emailUID, uploadedFile.downloadUrl, uploadedFile.fileName)
      objectSummary.map { summary =>
        latestUploadedFiles = latestUploadedFiles :+ uploadedFile.copy(objectStoreUrl = Some(summary.location.asUri),
          devHubUrl = Some("https://devhub.url/" + summary.location.asUri)
        )
        val finalUploadedFiles = email.attachmentDetails match {
          case Some(currentFiles) => currentFiles ++ latestUploadedFiles
          case None => latestUploadedFiles
        }
        updateEmailWithAttachments(email, finalUploadedFiles)
      }
    })
  }

  private def updateEmailWithAttachments(email: Email, finalUploadedFiles: Seq[UploadedFileWithObjectStore]) = {
    val er = EmailRequest(
      email.recipients,
      templateId = "gatekeeper",
      EmailData(email.subject, email.markdownEmailBody),
      attachmentDetails = Some(finalUploadedFiles))
    emailService.updateEmail(er, email.emailUID)
  }

  def deleteFilesFromObjectStoreAndUpdateEmailRecord(email: Email, filesToDeleteFromObjStore: Seq[UploadedFileWithObjectStore]) = {
    filesToDeleteFromObjStore.foreach(uploadedFile => {
        objectStoreService.deleteFromObjectStore(email.emailUID, uploadedFile.fileName)
        val finalUploadedFiles = email.attachmentDetails match {
          case Some(currentFiles) =>
            currentFiles.filterNot(file => file.upscanReference == uploadedFile.upscanReference)
          case None => List()
        }
      updateEmailWithAttachments(email, finalUploadedFiles)
    })
  }

  def filesToUploadInObjectStore(existingFiles: Option[Seq[UploadedFileWithObjectStore]],
                                uploadedFiles: Seq[UploadedFileWithObjectStore]):
  Seq[UploadedFileWithObjectStore] = {
    existingFiles match {
      case Some(current) =>
          val intersection = current.map(file => file.upscanReference).intersect(uploadedFiles.map(file => file.upscanReference))
          if (intersection.size == uploadedFiles.size) {
            List()
          } else {
            val diffRefs = uploadedFiles.map(file => file.upscanReference).diff(current.map(file => file.upscanReference))
            if(diffRefs.nonEmpty) {
              uploadedFiles.filter(file => diffRefs.contains(file.upscanReference))
            } else {
              List()
            }
          }
      case None => uploadedFiles
    }
  }

  def filesToRemoveFromObjectStore(existingFiles: Option[Seq[UploadedFileWithObjectStore]],
                                 uploadedFiles: Seq[UploadedFileWithObjectStore]):
  Seq[UploadedFileWithObjectStore] = {
    existingFiles match {
      case Some(current) =>
          val diffRefs = current.map(file => file.upscanReference).diff(uploadedFiles.map(file => file.upscanReference))
          if(diffRefs.nonEmpty) {
            current.filter(file => diffRefs.contains(file.upscanReference))
          } else {
            List()
          }
      case None => List()
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
