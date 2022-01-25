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

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailService @Inject()(emailConnector: GatekeeperEmailConnector,
                             emailRendererConnector: GatekeeperEmailRendererConnector,
                               emailRepository: EmailRepository)
                                           (implicit val ec: ExecutionContext) {

  val logger: Logger = Logger(getClass.getName)

  def persistEmail(emailRequest: EmailRequest): Future[Email] = {
    val email: Email = emailData(emailRequest)
    logger.info(s"email data  before saving $email")
    val parameters: Map[String, String] = Map("subject" -> s"${emailRequest.emailData.emailSubject}",
      "fromAddress" -> "gateKeeper",
      "body" -> s"${emailRequest.emailData.emailBody}",
      "service" -> "gatekeeper")
    val sendEmailRequest = SendEmailRequest(emailRequest.to, emailRequest.templateId, parameters, emailRequest.force,
      emailRequest.auditData, emailRequest.eventUrl)

    for {
      renderResult <- emailRendererConnector.getTemplatedEmail(sendEmailRequest)
      emailBody = getEmailBody(renderResult)
      templatedData = EmailTemplateData(sendEmailRequest.templateId, sendEmailRequest.parameters, sendEmailRequest.force,
        sendEmailRequest.auditData, sendEmailRequest.eventUrl)
      renderedEmail = email.copy(templateData = templatedData, htmlEmailBody = emailBody._1, markdownEmailBody = emailBody._2)
      _ <- emailRepository.persist(renderedEmail)
    } yield renderedEmail
  }

  def sendEmail(emailSaved: EmailSaved): Future[Email] = {
    for {
      email <- emailRepository.getEmailData(emailSaved)
      emailRequestedData = SendEmailRequest(email.recepients, email.templateData.templateId, email.templateData.parameters,
        email.templateData.force, email.templateData.auditData, email.templateData.eventUrl)
      _ <- emailConnector.sendEmail(emailRequestedData)
    } yield email

  }

  private def emailData(emailRequest: EmailRequest): Email = {
    val recepientsTitle = "TL API PLATFORM TEAM"

    val emailTemplateData = EmailTemplateData(emailRequest.templateId, Map(), emailRequest.force,
      emailRequest.auditData, emailRequest.eventUrl)

    Email(UUID.randomUUID().toString, emailTemplateData, recepientsTitle, emailRequest.to, None,
      emailRequest.emailData.emailBody, emailRequest.emailData.emailBody,
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