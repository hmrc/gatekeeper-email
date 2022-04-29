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

import javax.inject.{Inject, Singleton}
import play.api.{Logger, Logging}
import uk.gov.hmrc.gatekeeperemail.connectors.{GatekeeperEmailConnector, GatekeeperEmailRendererConnector}
import uk.gov.hmrc.gatekeeperemail.models._
import uk.gov.hmrc.gatekeeperemail.repositories.{DraftEmailRepository, SentEmailRepository}
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SentEmailService@Inject()(emailConnector: GatekeeperEmailConnector,
                                 draftEmailService: EmailService,
                                 sentEmailRepository: SentEmailRepository)
                                (implicit val ec: ExecutionContext) extends Logging {

  def updateEmailToSent(email:SentEmail): Future[SentEmail] = {
    sentEmailRepository.markSent(email)
  }

  def handleEmailSendingFailed(email:SentEmail): Future[SentEmail] = {
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

  def sendEmailScheduled (email: DraftEmail): Future[Int] = {
    val emailRequestedData = SendEmailRequest(email.recipients, email.templateData.templateId, email.templateData.parameters,
      email.templateData.force, email.templateData.auditData, email.templateData.eventUrl)
    emailConnector.sendEmail(emailRequestedData)
  }

  def fetchEmail(emailUUID: String): Future[DraftEmail] = {
    for {
      email <- draftEmailService.fetchEmail(emailUUID)
    } yield email
  }
}