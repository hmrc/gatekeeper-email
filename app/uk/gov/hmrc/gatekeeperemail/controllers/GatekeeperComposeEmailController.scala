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

package uk.gov.hmrc.gatekeeperemail.controllers

import org.joda.time.DateTime
import play.api.libs.json.JsValue
import play.api.mvc.{Action, MessagesControllerComponents, PlayBodyParsers, Result}
import uk.gov.hmrc.gatekeeperemail.connectors.GatekeeperEmailConnector
import uk.gov.hmrc.gatekeeperemail.models.{Email, EmailRequest, ErrorCode, JsErrorResponse}
import uk.gov.hmrc.gatekeeperemail.services.EmailService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class GatekeeperComposeEmailController @Inject()(
  mcc: MessagesControllerComponents,
  emailConnector: GatekeeperEmailConnector,
  playBodyParsers: PlayBodyParsers,
  emailService: EmailService
  )(implicit val ec: ExecutionContext)
    extends BackendController(mcc) with WithJson {

  def sendEmail: Action[JsValue] = Action.async(playBodyParsers.json) { implicit request =>
    withJson[EmailRequest] { receiveEmailRequest =>
      val recepientsTitle = "TL API PLATFORM TEAM"
//      val emailTo: String = "test.user@digital.hmrc.gov.uk"
//      val params: Map[String, String] = Map("subject" -> "subject",
//        "fromAddress" -> "gateKeeper",
//        "body" -> "Body to be used in the email template",
//        "service" -> "gatekeeper")
      val email = Email(recepientsTitle, receiveEmailRequest.to, None, "markdownEmailBody", Some(receiveEmailRequest.emailData.emailBody),
    receiveEmailRequest.emailData.emailSubject, "composedBy",
        Some("approvedBy"), DateTime.now())
      for {
          result <- emailService.saveEmail(email)
          sent <- emailConnector.sendEmail(receiveEmailRequest)
        } yield Ok("Email sent successfully")
    } recover recovery
  }

  private def recovery: PartialFunction[Throwable, Result] = {
    case e: IllegalArgumentException =>
      logger.info(s"Invalid request due to ${e.getMessage}")
      BadRequest(JsErrorResponse(ErrorCode.INVALID_REQUEST_PAYLOAD, e.getMessage))
  }
}
