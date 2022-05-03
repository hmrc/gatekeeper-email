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

   def sendAllPendingEmails: Future[Int] = {

     def updateEmailStatusToSent(email:SentEmail): Future[SentEmail] = {
       sentEmailRepository.markSent(email)
     }

     def handleEmailSendingFailed(email:SentEmail): Future[SentEmail] = {
       logger.info(s"Handling failed message for email with failedCount of ${email.failedCount}")
       if (email.failedCount > 2)  {
         sentEmailRepository.markFailed(email)
       } else {
         logger.info(s"Incrementing failed counter for email ${email}")
         sentEmailRepository.incrementFailedCount(email)
       }
     }

     def findAndSendNextEmail(sentEmail: SentEmail): Future[Int] = {
       logger.info(s"Fetching template with UUID ${sentEmail.emailUuid}")
       for {
         email <- fetchDraftEmailData(sentEmail.emailUuid.toString)
         result <- sendEmail(email)
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

     def sendEmail (email: DraftEmail): Future[Int] = {
       logger.info(s"Retrieved email wth id ${email.emailUUID}")
       val emailRequestedData = SendEmailRequest(email.recipients, email.templateData.templateId, email.templateData.parameters,
         email.templateData.force, email.templateData.auditData, email.templateData.eventUrl)
       logger.info(s"Sending email ${emailRequestedData}")
       emailConnector.sendEmail(emailRequestedData)
     }

     findNextEmail.flatMap {
      case None => Future.successful(0)
      case Some(sentEmail:SentEmail) => findAndSendNextEmail(sentEmail).map {status =>
        status match {
          case ACCEPTED => updateEmailStatusToSent(sentEmail)
          case _ => handleEmailSendingFailed(sentEmail)
        }
      }  map (_ => 1)
    }
  }
}
