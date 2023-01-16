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

package uk.gov.hmrc.gatekeeperemail.controllers.actions

import play.api.libs.json.JsValue
import play.api.mvc._
import uk.gov.hmrc.gatekeeperemail.controllers.GatekeeperBaseController
import uk.gov.hmrc.gatekeeperemail.stride.domain.models.GatekeeperRole

import scala.concurrent.Future

trait AuthorisationActions {
  self: GatekeeperBaseController =>

  private def strideRoleJsValue(minimumGatekeeperRole: GatekeeperRole.GatekeeperRole)(block: MessagesRequest[JsValue] => Future[Result]): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
       gatekeeperRoleActionRefiner(minimumGatekeeperRole)
      .invokeBlock(requestConverter.convert(request), block)
    }

  def loggedInJsValue()(block: Request[JsValue] => Future[Result]): Action[JsValue] = strideRoleJsValue(GatekeeperRole.USER)(block)

  private def strideRoleAnyContent(minimumGatekeeperRole: GatekeeperRole.GatekeeperRole)(block: MessagesRequest[AnyContent] => Future[Result]): Action[AnyContent] =
    Action.async { implicit request =>
       gatekeeperRoleActionRefiner(minimumGatekeeperRole)
      .invokeBlock(requestConverter.convert(request), block)
    }

  def loggedInAnyContent()(block: Request[AnyContent] => Future[Result]): Action[AnyContent] = strideRoleAnyContent(GatekeeperRole.USER)(block)
}
