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

package uk.gov.hmrc.gatekeeperemail.services

import org.joda.time.DateTime
import play.api.Logger
import uk.gov.hmrc.gatekeeperemail.connectors.GatekeeperEmailConnector
import uk.gov.hmrc.gatekeeperemail.models.{Email, EmailRequest}
import uk.gov.hmrc.gatekeeperemail.repositories.EmailRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailService @Inject()(emailConnector: GatekeeperEmailConnector,
                               emailRepository: EmailRepository)
                                           (implicit val ec: ExecutionContext) {

  val logger: Logger = Logger(getClass.getName)

  def saveEmail(receiveEmailRequest: EmailRequest): Future[Email] = {
    val recepientsTitle = "TL API PLATFORM TEAM"
    val email = Email(recepientsTitle, receiveEmailRequest.to, None, "markdownEmailBody", Some(receiveEmailRequest.emailData.emailBody),
      receiveEmailRequest.emailData.emailSubject, "composedBy",
      Some("approvedBy"), DateTime.now())
    logger.info(s"*******email before saving $email")
    for {
      _ <- emailConnector.sendEmail(receiveEmailRequest)
      _ <- emailRepository.persist(email)
    } yield email
  }
}