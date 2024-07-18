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
    def updateEmailStatusToSent(email: SentEmail): Future[String] = {
      sentEmailRepository.markSent(email).map(_ => "Sent successfully")
    }

    def handleEmailSendingFailed(email: SentEmail): Future[String] = {
      if (email.failedCount > 2) {
        logger.info(s"Marking email with id ${email.id} as failed as sending it has failed 3 times")
        sentEmailRepository.markFailed(email).map(_ => "Sending failed, giving up")
      } else {
        logger.info(s"Email with id ${email.id} has now failed to send ${email.failedCount + 1} times")
        sentEmailRepository.incrementFailedCount(email).map(_ => "Sending failed, retrying")
      }
    }

    def findAndSendNextEmail(sentEmail: SentEmail): Future[String] = {
      logger.debug(s"Fetching template with UUID ${sentEmail.emailUuid} to send email with UUID ${sentEmail.id}")
      val tags = Map[String, String]("regime" -> "API Platform", "template" -> "gatekeeper", "messageId" -> sentEmail.emailUuid.toString)

      (for {
        draftEmail                 <- draftEmailService.fetchEmail(sentEmail.emailUuid.toString)
        parametersWithRecipientName = draftEmail.templateData.parameters +
                                        ("firstName"      -> s"${sentEmail.firstName}") +
                                        ("lastName"       -> s"${sentEmail.lastName}") +
                                        ("showFooter"     -> "true") +
                                        ("showHmrcBanner" -> "true")
        emailRequestData            = SendEmailRequest(
                                        sentEmail.recipient,
                                        draftEmail.templateData.templateId,
                                        parametersWithRecipientName,
                                        draftEmail.templateData.force,
                                        draftEmail.templateData.auditData,
                                        draftEmail.templateData.eventUrl,
                                        tags
                                      )
        result                     <- emailConnector.sendEmail(emailRequestData)
        message                    <- if (result) updateEmailStatusToSent(sentEmail) else handleEmailSendingFailed(sentEmail)
      } yield message).recoverWith(_ => handleEmailSendingFailed(sentEmail))
    }

    sentEmailRepository.findNextEmailToSend.flatMap {
      case None            => Future.successful("No emails to send")
      case Some(sentEmail) => findAndSendNextEmail(sentEmail)
    }
  }
}
