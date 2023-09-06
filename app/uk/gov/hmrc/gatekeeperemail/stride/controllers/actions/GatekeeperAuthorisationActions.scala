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

package uk.gov.hmrc.gatekeeperemail.stride.controllers.actions

import scala.concurrent.{ExecutionContext, Future}

import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController

import uk.gov.hmrc.gatekeeperemail.controllers.RequestConverter
import uk.gov.hmrc.gatekeeperemail.stride.config.StrideAuthConfig
import uk.gov.hmrc.gatekeeperemail.stride.controllers.models.LoggedInRequest
import uk.gov.hmrc.gatekeeperemail.stride.domain.models.GatekeeperRole

trait ForbiddenHandler {
  def handle(msgResult: Request[_]): Result
}

trait GatekeeperAuthorisationActions {
  self: BackendBaseController =>

  def authConnector: AuthConnector

  def forbiddenHandler: ForbiddenHandler

  def strideAuthConfig: StrideAuthConfig

  def requestConverter: RequestConverter

  implicit def ec: ExecutionContext

  def gatekeeperRoleActionRefiner(minimumRoleRequired: GatekeeperRole): ActionRefiner[MessagesRequest, LoggedInRequest] =
    new ActionRefiner[MessagesRequest, LoggedInRequest] {
      def executionContext = ec

      def refine[A](msgRequest: MessagesRequest[A]): Future[Either[Result, LoggedInRequest[A]]] = {
        val successUrl = s"${strideAuthConfig.successUrlBase}${msgRequest.uri}"

        lazy val loginRedirect =
          Redirect(
            strideAuthConfig.strideLoginUrl,
            Map("successURL" -> Seq(successUrl), "origin" -> Seq(strideAuthConfig.origin))
          )

        implicit val request = msgRequest

        val predicate = authPredicate(minimumRoleRequired)
        val retrieval = Retrievals.name and Retrievals.authorisedEnrolments

        authConnector.authorise(predicate, retrieval) map {
          case Some(name) ~ authorisedEnrolments => Right(new LoggedInRequest(name.name, authorisedEnrolments, convertRequest(request)))
          case None ~ authorisedEnrolments       => Left(forbiddenHandler.handle(msgRequest))
        } recover {
          case _: NoActiveSession        => Left(loginRedirect)
          case _: InsufficientEnrolments => Left(forbiddenHandler.handle(msgRequest))
        }
      }
    }

  private def authPredicate(minimumRoleRequired: GatekeeperRole): Predicate = {
    val adminEnrolment     = Enrolment(strideAuthConfig.adminRole)
    val superUserEnrolment = Enrolment(strideAuthConfig.superUserRole)
    val userEnrolment      = Enrolment(strideAuthConfig.userRole)

    minimumRoleRequired match {
      case GatekeeperRole.ADMIN     => adminEnrolment
      case GatekeeperRole.SUPERUSER => adminEnrolment or superUserEnrolment
      case GatekeeperRole.USER      => adminEnrolment or superUserEnrolment or userEnrolment
    }
  }

  private def gatekeeperRoleAction(minimumRoleRequired: GatekeeperRole)(block: LoggedInRequest[_] => Future[Result]): Action[AnyContent] =
    Action.async { implicit request =>
      gatekeeperRoleActionRefiner(minimumRoleRequired).invokeBlock(convertRequest(request), block)
    }

  def anyStrideUserAction(block: LoggedInRequest[_] => Future[Result]): Action[AnyContent] =
    gatekeeperRoleAction(GatekeeperRole.USER)(block)

  def convertRequest[A](request: Request[A]): MessagesRequest[A] = {
    requestConverter.convert(request)
  }
}
