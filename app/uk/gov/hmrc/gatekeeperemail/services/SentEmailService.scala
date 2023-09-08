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

package uk.gov.hmrc.gatekeeperemail.services

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import cats.data.EitherT

import play.api.Logging

import uk.gov.hmrc.gatekeeperemail.connectors.EmailConnector
import uk.gov.hmrc.gatekeeperemail.models._
import uk.gov.hmrc.gatekeeperemail.models.requests.SendEmailRequest
import uk.gov.hmrc.gatekeeperemail.repositories.SentEmailRepository

@Singleton
class SentEmailService @Inject() (
    emailConnector: EmailConnector,
    draftEmailService: DraftEmailService,
    sentEmailRepository: SentEmailRepository
  )(implicit val ec: ExecutionContext
  ) extends Logging {

  def sendNextPendingEmail: Future[String] = {
    def updateEmailStatusToSent(email: SentEmail): Future[Either[String, String]] = {
      sentEmailRepository.markSent(email).map(_ => Right("Sent successfully"))
    }

    def handleEmailSendingFailed(email: SentEmail): Future[Either[String, String]] = {
      if (email.failedCount > 2) {
        logger.info(s"Marking email with id ${email.id} as failed as sending it has failed 3 times")
        sentEmailRepository.markFailed(email).map(_ => Left("Sending failed, giving up"))
      } else {
        logger.info(s"Email with id ${email.id} has now failed to send ${email.failedCount + 1} times")
        sentEmailRepository.incrementFailedCount(email).map(_ => Left("Sending failed, retrying"))
      }
    }

    def findAndSendNextEmail(sentEmail: SentEmail): Future[Either[String, Boolean]] = {
      logger.debug(s"Fetching template with UUID ${sentEmail.emailUuid} to send email with UUID ${sentEmail.id}")
      val tags = Map[String, String]("regime" -> "API Platform", "template" -> "gatekeeper", "messageId" -> sentEmail.emailUuid.toString)

      for {
        email                      <- fetchDraftEmailData(sentEmail.emailUuid.toString)
        parametersWithRecipientName = buildEmailTemplateParameters(sentEmail, email.templateData.parameters)
        emailRequestData            = SendEmailRequest(
                                        sentEmail.recipient,
                                        email.templateData.templateId,
                                        parametersWithRecipientName,
                                        email.templateData.force,
                                        email.templateData.auditData,
                                        email.templateData.eventUrl,
                                        tags
                                      )
        result                     <- sendEmail(emailRequestData)
      } yield Right(result)
    }

    def fetchDraftEmailData(emailUUID: String): Future[DraftEmail] = {
      for {
        email <- draftEmailService.fetchEmail(emailUUID)
      } yield email
    }

    def findNextEmail: Future[Either[String, SentEmail]] = {
      sentEmailRepository.findNextEmailToSend.map {
        case Some(sentEmail) => Right(sentEmail)
        case None            => Left("No emails to send")
      }
    }

    def sendEmail(emailRequest: SendEmailRequest): Future[Boolean] = {
      emailConnector.sendEmail(emailRequest)
    }

    (for {
      sentEmail <- EitherT(findNextEmail)
      status    <- EitherT(findAndSendNextEmail(sentEmail))
      message   <- EitherT(if (status) updateEmailStatusToSent(sentEmail) else handleEmailSendingFailed(sentEmail))
    } yield message)
      .value.map {
        case Right(message) => message
        case Left(message)  => message
      }
  }

  private def buildEmailTemplateParameters(sentEmail: SentEmail, parameters: Map[String, String]): Map[String, String] = {
    parameters +
      ("firstName"      -> s"${sentEmail.firstName}") +
      ("lastName"       -> s"${sentEmail.lastName}") +
      ("showFooter"     -> "true") +
      ("showHmrcBanner" -> "true")
  }
}
