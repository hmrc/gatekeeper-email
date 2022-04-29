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

package uk.gov.hmrc.gatekeeperemail.services

import java.time.LocalDateTime
import java.util.UUID

import akka.Done
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.http.Status.OK
import uk.gov.hmrc.gatekeeperemail.connectors.{GatekeeperEmailConnector, GatekeeperEmailRendererConnector}
import uk.gov.hmrc.gatekeeperemail.models.EmailStatus.IN_PROGRESS
import uk.gov.hmrc.gatekeeperemail.models._
import uk.gov.hmrc.gatekeeperemail.repositories.{DraftEmailRepository, SentEmailRepository}
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailService @Inject()(emailConnector: GatekeeperEmailConnector,
                             emailRendererConnector: GatekeeperEmailRendererConnector,
                             draftEmailRepository: DraftEmailRepository,
                             sentEmailRepository: SentEmailRepository)
                                           (implicit val ec: ExecutionContext) {

  val logger: Logger = Logger(getClass.getName)

  def persistEmail(emailRequest: EmailRequest, emailUUID: String): Future[DraftEmail] = {
    val email: DraftEmail = emailData(emailRequest, emailUUID)

    val sendEmailRequest = SendEmailRequest(emailRequest.to, emailRequest.templateId, email.templateData.parameters, emailRequest.force,
      emailRequest.auditData, emailRequest.eventUrl)

    for {
      renderResult <- emailRendererConnector.getTemplatedEmail(sendEmailRequest)
      emailBody = getEmailBody(renderResult)
      templatedData = EmailTemplateData(sendEmailRequest.templateId, sendEmailRequest.parameters, sendEmailRequest.force,
        sendEmailRequest.auditData, sendEmailRequest.eventUrl)
      renderedEmail = email.copy(templateData = templatedData, htmlEmailBody = emailBody._1, markdownEmailBody = emailBody._2)
      _ <- draftEmailRepository.persist(renderedEmail)
    } yield renderedEmail
  }

  def updateEmailToSent(email:SentEmail) = {
    sentEmailRepository.markSent(email)
  }

  def handleEmailSendingFailed(email:SentEmail) = {
    if (email.failedCount > 3)  {
      sentEmailRepository.markFailed(email)
    } else {
      sentEmailRepository.incrementFailedCount(email)
    }
  }

  def sendEmails = {
    findNextEmail.flatMap {
      case None => Future.successful(0)
      case Some(sentEmail:SentEmail) => findAndSendNextEmail(sentEmail).map {status => status match {
          case 200 => updateEmailToSent(sentEmail)
          case _ => handleEmailSendingFailed(sentEmail)
        }
      }
    }
  }

 def findAndSendNextEmail(sentEmail: SentEmail): Future[Int] = {
   for {
     email <- fetchEmail(sentEmail.emailUuid.toString)
     result <- sendEmailScheduled(email)
   } yield result
 }

  def findNextEmail: Future[Option[SentEmail]] = {
    sentEmailRepository.findNextEmailToSend
  }

  def sendEmail(emailUUID: String): Future[DraftEmail] = {
    for {
      email <- draftEmailRepository.getEmailData(emailUUID)
      emailRequestedData = SendEmailRequest(email.recipients, email.templateData.templateId, email.templateData.parameters,
        email.templateData.force, email.templateData.auditData, email.templateData.eventUrl)
      _ <- emailConnector.sendEmail(emailRequestedData)
      _ <- draftEmailRepository.updateEmailSentStatus(emailUUID)
    } yield email
  }

  def sendEmailScheduled (email: DraftEmail): Future[Int] = {
    val emailRequestedData = SendEmailRequest(email.recipients, email.templateData.templateId, email.templateData.parameters,
      email.templateData.force, email.templateData.auditData, email.templateData.eventUrl)
    emailConnector.sendEmail(emailRequestedData)
  }

  def fetchEmail(emailUUID: String): Future[DraftEmail] = {
    for {
      email <- draftEmailRepository.getEmailData(emailUUID)
    } yield email
  }

  def deleteEmail(emailUUID: String): Future[Boolean] = {
    draftEmailRepository.deleteByemailUUID(emailUUID)
  }

  def updateEmail(emailRequest: EmailRequest, emailUUID: String): Future[DraftEmail] = {
    val email: DraftEmail = emailData(emailRequest, emailUUID)

    val sendEmailRequest = SendEmailRequest(emailRequest.to, emailRequest.templateId, email.templateData.parameters, emailRequest.force,
      emailRequest.auditData, emailRequest.eventUrl)

    for {
      renderResult <- emailRendererConnector.getTemplatedEmail(sendEmailRequest)
      emailBody = getEmailBody(renderResult)
      templatedData = EmailTemplateData(sendEmailRequest.templateId, sendEmailRequest.parameters, sendEmailRequest.force,
        sendEmailRequest.auditData, sendEmailRequest.eventUrl)
      renderedEmail = email.copy(templateData = templatedData, htmlEmailBody = emailBody._1,
        markdownEmailBody = emailBody._2, subject = emailRequest.emailData.emailSubject)
      _ <- draftEmailRepository.updateEmail(renderedEmail)
    } yield renderedEmail
  }

  private def emailData(emailRequest: EmailRequest, emailUUID: String): DraftEmail = {
    val recipientsTitle = "TL API PLATFORM TEAM"
    val parameters: Map[String, String] = Map("subject" -> s"${emailRequest.emailData.emailSubject}",
      "fromAddress" -> "gateKeeper",
      "body" -> s"${emailRequest.emailData.emailBody}",
      "service" -> "gatekeeper",
      "firstName" -> "((first name))",
      "lastName" -> "((last name))",
      "showFooter" -> "false",
      "showHmrcBanner" -> "false")
    val emailTemplateData = EmailTemplateData(emailRequest.templateId, parameters, emailRequest.force,
      emailRequest.auditData, emailRequest.eventUrl)

    DraftEmail(emailUUID,  emailTemplateData, recipientsTitle, emailRequest.to, emailRequest.attachmentDetails,
      emailRequest.emailData.emailBody, emailRequest.emailData.emailBody,
      emailRequest.emailData.emailSubject, EmailStatus.IN_PROGRESS.toString, "composedBy",
      Some("approvedBy"), LocalDateTime.now())
  }

  private def getEmailBody(rendererResult: Either[UpstreamErrorResponse, RenderResult]) = {
    rendererResult match {
      case Left(UpstreamErrorResponse(message, _, _, _)) =>
        throw new EmailRendererConnectionError(message)
      case Right(result: RenderResult) =>
        Tuple2(result.html, result.plain)
    }
  }
}