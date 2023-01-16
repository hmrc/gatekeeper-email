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
import play.api.Logging
import play.api.http.Status.ACCEPTED
import uk.gov.hmrc.gatekeeperemail.connectors.GatekeeperEmailConnector
import uk.gov.hmrc.gatekeeperemail.models._
import uk.gov.hmrc.gatekeeperemail.repositories.SentEmailRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SentEmailService@Inject()(emailConnector: GatekeeperEmailConnector,
                                draftEmailService: DraftEmailService,
                                sentEmailRepository: SentEmailRepository)
                                (implicit val ec: ExecutionContext) extends Logging {

   def sendNextPendingEmail: Future[Int] = {
     def updateEmailStatusToSent(email:SentEmail): Future[SentEmail] = {
       sentEmailRepository.markSent(email)
     }

     def handleEmailSendingFailed(email:SentEmail): Future[SentEmail] = {
       if (email.failedCount > 2)  {
         logger.info(s"Marking email with id ${email.id} as failed as sending it has failed 3 times")
         sentEmailRepository.markFailed(email)
       } else {
         logger.info(s"Email with id ${email.id} has now failed to send ${email.failedCount + 1} times")
         sentEmailRepository.incrementFailedCount(email)
       }
     }

     def findAndSendNextEmail(sentEmail: SentEmail): Future[Int] = {
      logger.debug(s"Fetching template with UUID ${sentEmail.emailUuid} to send email with UUID ${sentEmail.id}")
       val tags = Map[String, String]("regime" -> "API Platform", "template" -> "gatekeeper", "messageId" -> sentEmail.emailUuid.toString)

       for {
        email <- fetchDraftEmailData(sentEmail.emailUuid.toString)
        parametersWithRecipientName = buildEmailTemplateParameters(sentEmail, email.templateData.parameters)
        emailRequestData = SendEmailRequest(sentEmail.recipient, email.templateData.templateId, parametersWithRecipientName,
          email.templateData.force, email.templateData.auditData, email.templateData.eventUrl, tags)
        result <- sendEmail(emailRequestData)
      } yield result
    }

     def fetchDraftEmailData(emailUUID: String): Future[DraftEmail] = {
       for {
         email <- draftEmailService.fetchEmail(emailUUID)
       } yield email
     }

      def findNextEmail: Future[Option[SentEmail]] = {
       sentEmailRepository.findNextEmailToSend
     }

     def sendEmail (emailRequest: SendEmailRequest): Future[Int] = {
       emailConnector.sendEmail(emailRequest)
     }

     findNextEmail.flatMap {
      case None => Future.successful(0)
      case Some(sentEmail:SentEmail) => findAndSendNextEmail(sentEmail).map {
          case ACCEPTED => updateEmailStatusToSent(sentEmail)
          case _ => handleEmailSendingFailed(sentEmail)
        } map (_ => 1)
      }
  }

  private def buildEmailTemplateParameters(sentEmail: SentEmail, parameters: Map[String, String]):Map[String, String] = {
    parameters +
      ("firstName" -> s"${sentEmail.firstName}") +
      ("lastName" -> s"${sentEmail.lastName}") +
      ("showFooter" -> "true") +
      ("showHmrcBanner" -> "true")
  }
}
