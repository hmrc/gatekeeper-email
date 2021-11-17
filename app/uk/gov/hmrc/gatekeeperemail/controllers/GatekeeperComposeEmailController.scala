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

import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.connectors.GatekeeperEmailConnector
import uk.gov.hmrc.gatekeeperemail.models.SendEmailRequest
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GatekeeperComposeEmailController @Inject()(
  mcc: MessagesControllerComponents,
  emailConnector: GatekeeperEmailConnector,
  )(implicit val appConfig: AppConfig, val ec: ExecutionContext)
    extends BackendController(mcc)  {

  val sendEmail: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[SendEmailRequest] { emailRequest =>

      emailConnector.sendEmail(emailRequest)
      Future.successful(Ok("Email sent successfully"))
    }
  }

}
