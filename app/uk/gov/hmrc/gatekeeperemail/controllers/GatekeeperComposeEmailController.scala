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

import java.io.IOException
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import play.api.mvc._
import uk.gov.hmrc.objectstore.client.ObjectSummaryWithMd5

import uk.gov.hmrc.gatekeeperemail.controllers.actions.AuthorisationActions
import uk.gov.hmrc.gatekeeperemail.models.{UploadedFileWithObjectStore, _}
import uk.gov.hmrc.gatekeeperemail.services.{DraftEmailService, ObjectStoreService}
import uk.gov.hmrc.gatekeeperemail.stride.config.StrideAuthConfig
import uk.gov.hmrc.gatekeeperemail.stride.connectors.AuthConnector
import uk.gov.hmrc.gatekeeperemail.stride.controllers.actions.ForbiddenHandler

@Singleton
class GatekeeperComposeEmailController @Inject() (
    strideAuthConfig: StrideAuthConfig,
    authConnector: AuthConnector,
    forbiddenHandler: ForbiddenHandler,
    requestConverter: RequestConverter,
    mcc: MessagesControllerComponents,
    emailService: DraftEmailService,
    objectStoreService: ObjectStoreService
  )(implicit override val ec: ExecutionContext
  ) extends GatekeeperBaseController(strideAuthConfig, authConnector, forbiddenHandler, requestConverter, mcc) with AuthorisationActions {

  def saveEmail(emailUUID: String): Action[JsValue] = loggedInJsValue() { implicit request =>
    withJsonBody[EmailRequest] { receiveEmailRequest =>
      emailService.persistEmail(receiveEmailRequest, emailUUID)
        .map(email => Ok(toJson(outgoingEmail(email))))
        .recover(recovery)
    }
  }

  def updateEmail(emailUUID: String): Action[JsValue] = loggedInJsValue() { implicit request =>
    withJsonBody[EmailRequest] { receiveEmailRequest =>
      emailService.updateEmail(receiveEmailRequest, emailUUID)
        .map(email => Ok(toJson(outgoingEmail(email))))
        .recover(recovery)
    }
  }

  def updateFiles(): Action[JsValue] = loggedInJsValue() { implicit request =>
    withJsonBody[UploadedFileMetadata] { value: UploadedFileMetadata =>
      logger.info(s"******UPDATE FILES ******")
      val fetchEmail: Future[DraftEmail] = emailService.fetchEmail(emailUUID = value.cargo.get.emailUUID)
      fetchEmail.map { email =>
        val filesToUploadInObjStore: Seq[UploadedFileWithObjectStore]   =
          filesToUploadInObjectStore(email.attachmentDetails, value.uploadedFiles)
        logger.info(s"*****FILES TO UPLOAD IN OBJECT STORE ******")
        uploadFilesToObjectStoreAndUpdateEmailRecord(email, filesToUploadInObjStore)
        val filesToDeleteFromObjStore: Seq[UploadedFileWithObjectStore] =
          filesToRemoveFromObjectStore(email.attachmentDetails, value.uploadedFiles)
        logger.info(s"*****FILES TO DELETE FROM OBJECT STORE **********")
        deleteFilesFromObjectStoreAndUpdateEmailRecord(email, filesToDeleteFromObjStore)
        NoContent
      }
    }.recover(recovery)
  }

  private def uploadFilesToObjectStoreAndUpdateEmailRecord(email: DraftEmail, filesToUploadInObjStore: Seq[UploadedFileWithObjectStore]) = {
    var latestUploadedFiles: Seq[UploadedFileWithObjectStore] = Seq.empty[UploadedFileWithObjectStore]
    filesToUploadInObjStore.foreach(uploadedFile => {
      val objectSummary: Future[ObjectSummaryWithMd5] = objectStoreService.uploadToObjectStore(email.emailUUID, uploadedFile.downloadUrl, uploadedFile.fileName)
      objectSummary.map { summary =>
        latestUploadedFiles =
          latestUploadedFiles :+ uploadedFile.copy(objectStoreUrl = Some(summary.location.asUri), devHubUrl = Some("https://devhub.url/" + summary.location.asUri))
        val finalUploadedFiles = email.attachmentDetails match {
          case Some(currentFiles) => currentFiles ++ latestUploadedFiles
          case None               => latestUploadedFiles
        }
        updateEmailWithAttachments(email, finalUploadedFiles)
      }
    })
  }

  private def updateEmailWithAttachments(email: DraftEmail, finalUploadedFiles: Seq[UploadedFileWithObjectStore]) = {
    val er = EmailRequest(
      email.userSelectionQuery,
      templateId = "gatekeeper",
      EmailData(email.subject, email.markdownEmailBody),
      attachmentDetails = Some(finalUploadedFiles)
    )
    emailService.updateEmail(er, email.emailUUID)
  }

  private def deleteFilesFromObjectStoreAndUpdateEmailRecord(email: DraftEmail, filesToDeleteFromObjStore: Seq[UploadedFileWithObjectStore]) = {
    filesToDeleteFromObjStore.foreach(uploadedFile => {
      objectStoreService.deleteFromObjectStore(email.emailUUID, uploadedFile.fileName)
      val finalUploadedFiles = email.attachmentDetails match {
        case Some(currentFiles) =>
          currentFiles.filterNot(file => file.upscanReference == uploadedFile.upscanReference)
        case None               => List()
      }
      updateEmailWithAttachments(email, finalUploadedFiles)
    })
  }

  def filesToUploadInObjectStore(existingFiles: Option[Seq[UploadedFileWithObjectStore]], uploadedFiles: Seq[UploadedFileWithObjectStore]): Seq[UploadedFileWithObjectStore] = {
    existingFiles match {
      case Some(current) =>
        val intersection = current.map(file => file.upscanReference).intersect(uploadedFiles.map(file => file.upscanReference))
        if (intersection.size == uploadedFiles.size) {
          List()
        } else {
          val diffRefs = uploadedFiles.map(file => file.upscanReference).diff(current.map(file => file.upscanReference))
          if (diffRefs.nonEmpty) {
            uploadedFiles.filter(file => diffRefs.contains(file.upscanReference))
          } else {
            List()
          }
        }
      case None          => uploadedFiles
    }
  }

  def filesToRemoveFromObjectStore(existingFiles: Option[Seq[UploadedFileWithObjectStore]], uploadedFiles: Seq[UploadedFileWithObjectStore]): Seq[UploadedFileWithObjectStore] = {
    existingFiles match {
      case Some(current) =>
        val diffRefs = current.map(file => file.upscanReference).diff(uploadedFiles.map(file => file.upscanReference))
        if (diffRefs.nonEmpty) {
          current.filter(file => diffRefs.contains(file.upscanReference))
        } else {
          List()
        }
      case None          => List()
    }
  }

  def fetchEmail(emailUUID: String): Action[AnyContent] = loggedInAnyContent() { implicit request =>
    logger.info(s"In fetchEmail for $emailUUID")
    emailService.fetchEmail(emailUUID)
      .map(email => Ok(toJson(outgoingEmail(email))))
      .recover(recovery)
  }

  def deleteEmail(emailUUID: String): Action[AnyContent] = loggedInAnyContent() { implicit request =>
    logger.info(s"In deleteEmail for $emailUUID")
    emailService.deleteEmail(emailUUID)
      .map(email =>
        Ok(toJson(email))
      )
      .recover(recovery)
  }

  def sendEmail(emailUUID: String): Action[AnyContent] = loggedInAnyContent() { implicit request =>
    emailService.sendEmail(emailUUID)
      .map(email => Ok(toJson(outgoingEmail(email))))
      .recover(recovery)
  }

  private def outgoingEmail(email: DraftEmail): OutgoingEmail = {
    OutgoingEmail(
      email.emailUUID,
      email.recipientTitle,
      email.userSelectionQuery,
      email.attachmentDetails,
      email.markdownEmailBody,
      email.htmlEmailBody,
      email.subject,
      email.status,
      email.composedBy,
      email.approvedBy,
      email.emailsCount
    )
  }

  private def recovery: PartialFunction[Throwable, Result] = {
    case e: IOException =>
      logger.error(s"IOException ${e.getMessage}")
      InternalServerError(JsErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage))
    case th: Throwable  =>
      logger.error(s"Throwable message : ${th.getMessage} and Throwable: $th")
      InternalServerError(JsErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, th.getMessage))
  }
}
