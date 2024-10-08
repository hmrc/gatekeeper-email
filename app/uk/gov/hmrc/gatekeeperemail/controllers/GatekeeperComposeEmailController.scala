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

package uk.gov.hmrc.gatekeeperemail.controllers

import java.io.IOException
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import play.api.mvc._

import uk.gov.hmrc.gatekeeperemail.controllers.actions.AuthorisationActions
import uk.gov.hmrc.gatekeeperemail.models.DraftEmail
import uk.gov.hmrc.gatekeeperemail.models.requests._
import uk.gov.hmrc.gatekeeperemail.models.responses.{ErrorCode, JsErrorResponse, OutgoingEmail}
import uk.gov.hmrc.gatekeeperemail.services.DraftEmailService
import uk.gov.hmrc.gatekeeperemail.stride.config.StrideAuthConfig
import uk.gov.hmrc.gatekeeperemail.stride.connectors.AuthConnector
import uk.gov.hmrc.gatekeeperemail.stride.controllers.actions.ForbiddenHandler

@Singleton
class GatekeeperComposeEmailController @Inject() (
    strideAuthConfig: StrideAuthConfig,
    authConnector: AuthConnector,
    forbiddenHandler: ForbiddenHandler,
    requestConverter: RequestConverter,
    mcc: MessagesControllerComponents,
    emailService: DraftEmailService
  )(implicit override val ec: ExecutionContext
  ) extends GatekeeperBaseController(strideAuthConfig, authConnector, forbiddenHandler, requestConverter, mcc) with AuthorisationActions {

  def saveEmail(emailUUID: String): Action[JsValue] = loggedInJsValue() { implicit request =>
    withJsonBody[EmailRequest] { receiveEmailRequest =>
      emailService.persistEmail(receiveEmailRequest, emailUUID)
        .map(email => Ok(toJson(outgoingEmail(email))))
        .recover(recovery)
    }
  }

  def updateEmail(emailUUID: String): Action[JsValue] = loggedInJsValue() { implicit request =>
    withJsonBody[EmailRequest] { receiveEmailRequest =>
      emailService.updateEmail(receiveEmailRequest, emailUUID)
        .map(email => Ok(toJson(outgoingEmail(email))))
        .recover(recovery)
    }
  }

  def fetchEmail(emailUUID: String): Action[AnyContent] = loggedInAnyContent() { _ =>
    logger.info(s"In fetchEmail for $emailUUID")
    emailService.fetchEmail(emailUUID)
      .map(email => Ok(toJson(outgoingEmail(email))))
      .recover(recovery)
  }

  def deleteEmail(emailUUID: String): Action[AnyContent] = loggedInAnyContent() { _ =>
    logger.info(s"In deleteEmail for $emailUUID")
    emailService.deleteEmail(emailUUID)
      .map(email =>
        Ok(toJson(email))
      )
      .recover(recovery)
  }

  def sendEmail(emailUUID: String): Action[AnyContent] = loggedInAnyContent() { _ =>
    emailService.sendEmail(emailUUID)
      .map(email => Ok(toJson(outgoingEmail(email))))
      .recover(recovery)
  }

  def sendTestEmail(emailUUID: String): Action[JsValue] = loggedInJsValue() { implicit request =>
    withJsonBody[TestEmailRequest] { req =>
      emailService.sendEmail(emailUUID, req.email)
        .map(email => Ok(toJson(outgoingEmail(email))))
    } recover recovery
  }

  private def outgoingEmail(email: DraftEmail): OutgoingEmail = {
    OutgoingEmail(
      email.emailUUID,
      email.recipientTitle,
      email.userSelectionQuery,
      email.markdownEmailBody,
      email.htmlEmailBody,
      email.subject,
      email.status,
      email.composedBy,
      email.approvedBy,
      email.emailsCount
    )
  }

  private def recovery: PartialFunction[Throwable, Result] = {
    case e: IOException =>
      logger.error(s"IOException ${e.getMessage}")
      InternalServerError(JsErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage))
    case th: Throwable  =>
      logger.error(s"Throwable message : ${th.getMessage} and Throwable: $th")
      InternalServerError(JsErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, th.getMessage))
  }
}
