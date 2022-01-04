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

import org.joda.time.DateTime
import org.mongodb.scala.bson.BsonValue
import play.api.Logger
import uk.gov.hmrc.gatekeeperemail.connectors.{GatekeeperEmailConnector, GatekeeperEmailRendererConnector}
import uk.gov.hmrc.gatekeeperemail.models._
import uk.gov.hmrc.gatekeeperemail.repositories.EmailRepository
import uk.gov.hmrc.http.UpstreamErrorResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailService @Inject()(emailConnector: GatekeeperEmailConnector,
                             emailRendererConnector: GatekeeperEmailRendererConnector,
                               emailRepository: EmailRepository)
                                           (implicit val ec: ExecutionContext) {

  val logger: Logger = Logger(getClass.getName)

  def sendAndPersistEmail(emailRequest: EmailRequest): Future[BsonValue] = {
    val email: Email = emailData(emailRequest)
    logger.info(s"*******email data  before saving $email")
    val parameters: Map[String, String] = Map("subject" -> s"${emailRequest.emailData.emailSubject}",
      "fromAddress" -> "gateKeeper",
      "body" -> s"${emailRequest.emailData.emailBody}",
      "service" -> "gatekeeper")
    val sendEmailRequest = SendEmailRequest(emailRequest.to, emailRequest.templateId, parameters, emailRequest.force,
      emailRequest.auditData, emailRequest.eventUrl)

    for {
      renderResult <- emailRendererConnector.getTemplatedEmail(sendEmailRequest)
      emailBody = getEmailBody(renderResult)
      _ <- emailConnector.sendEmail(sendEmailRequest)
      persistedEmail <- emailRepository.persist(email.copy(htmlEmailBody = Some(emailBody._1), markdownEmailBody = emailBody._2))
    } yield persistedEmail.getInsertedId
  }

  private def emailData(emailRequest: EmailRequest): Email = {
    val recepientsTitle = "TL API PLATFORM TEAM"
    Email(recepientsTitle, emailRequest.to, None, emailRequest.emailData.emailBody, Some(emailRequest.emailData.emailBody),
      emailRequest.emailData.emailSubject, "composedBy",
      Some("approvedBy"), DateTime.now())
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