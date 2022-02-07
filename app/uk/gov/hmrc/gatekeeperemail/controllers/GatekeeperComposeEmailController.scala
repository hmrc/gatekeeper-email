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

package uk.gov.hmrc.gatekeeperemail.controllers

import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, PlayBodyParsers, Result}
import uk.gov.hmrc.gatekeeperemail.models.{Email, EmailRequest, EmailSaved, ErrorCode, JsErrorResponse, OutgoingEmail}
import uk.gov.hmrc.gatekeeperemail.services.EmailService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.io.IOException
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class GatekeeperComposeEmailController @Inject()(
  mcc: MessagesControllerComponents,
  playBodyParsers: PlayBodyParsers,
  emailService: EmailService
  )(implicit val ec: ExecutionContext)
    extends BackendController(mcc) with WithJson {

  def saveEmail(emailUID: String, key: String): Action[JsValue] = Action.async(playBodyParsers.json) { implicit request =>
    withJson[EmailRequest] { receiveEmailRequest =>
      emailService.persistEmail(receiveEmailRequest, emailUID, key)
        .map(email => Ok(toJson(outgoingEmail(email))))
        .recover(recovery)
    }
  }

  def updateEmail(emailUID: String, key: String): Action[JsValue] = Action.async(playBodyParsers.json) { implicit request =>
    withJson[EmailRequest] { receiveEmailRequest =>
      emailService.updateEmail(receiveEmailRequest, EmailSaved(emailUID), key)
        .map(email => Ok(toJson(outgoingEmail(email))))
        .recover(recovery)
    }
  }

  def fetchEmail(emailUID: String): Action[AnyContent] = Action.async { implicit request =>
      logger.info(s"In fetchEmail for $emailUID")
      emailService.fetchEmail(EmailSaved(emailUID))
        .map(email => Ok(toJson(outgoingEmail(email))))
        .recover(recovery)
  }

  def sendEmail(emailUID: String): Action[AnyContent] = Action.async{ implicit request =>
    emailService.sendEmail(EmailSaved(emailUID))
      .map(email => Ok(toJson(outgoingEmail(email))))
      .recover(recovery)
  }

  private def outgoingEmail(email: Email): OutgoingEmail = {
    OutgoingEmail(email.emailUID, email.recipientTitle, email.recipients, email.attachmentLink,
      email.markdownEmailBody, email.htmlEmailBody, email.subject,
      email.composedBy, email.approvedBy)
  }

  private def recovery: PartialFunction[Throwable, Result] = {
    case e: IOException =>
      logger.warn(s"IOException ${e.getMessage}")
      InternalServerError(JsErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage))
  }
}
