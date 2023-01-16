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

package uk.gov.hmrc.gatekeeperemail.connectors

import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.Status
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import uk.gov.hmrc.gatekeeperemail.config.EmailConnectorConfig
import uk.gov.hmrc.gatekeeperemail.models.{OneEmailRequest, SendEmailRequest}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpClient, _}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class GatekeeperEmailConnector @Inject()(http: HttpClient, config: EmailConnectorConfig)(implicit ec: ExecutionContext)
  extends HttpErrorFunctions with Logging {

  private lazy val serviceUrl = config.emailBaseUrl

 def sendEmail(emailRequest: SendEmailRequest): Future[Int] = {
    implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(CONTENT_TYPE -> "application/json")

    postHttpRequest(emailRequest).map {
      case Left(UpstreamErrorResponse(_, statusCode, _, _)) =>
        logger.warn(s"Error $statusCode")
        statusCode
      case Right(status: Int) =>
        status
    }
      .recoverWith {
        case NonFatal(e) =>
          logger.warn(s"NonFatal error ${e.getMessage} while sending message to email service", e)
          Future.successful(Status.INTERNAL_SERVER_ERROR)
      }
  }

 private def postHttpRequest(request: SendEmailRequest)(implicit hc: HeaderCarrier): Future[Either[Throwable, Int]] = {
   val oneEmailRequest = OneEmailRequest(List(request.to), request.templateId, request.parameters, request.force,
     request.auditData, request.eventUrl, request.tags)
   http.POST[OneEmailRequest, HttpResponse](s"$serviceUrl/developer/email", oneEmailRequest).map {
     res => Right(res.status)
   }
   .recover  {
     case NonFatal(e) => logger.error(e.getMessage)
       Left(e)
   }
 }
}
