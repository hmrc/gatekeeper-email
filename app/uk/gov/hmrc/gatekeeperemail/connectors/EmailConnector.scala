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
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

import play.api.Logging
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.Status
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._

import uk.gov.hmrc.gatekeeperemail.config.EmailConnectorConfig
import uk.gov.hmrc.gatekeeperemail.models.requests.{OneEmailRequest, SendEmailRequest}

@Singleton
class EmailConnector @Inject() (http: HttpClient, config: EmailConnectorConfig)(implicit ec: ExecutionContext)
    extends HttpErrorFunctions with Logging {

  private lazy val serviceUrl = config.emailBaseUrl

  def sendEmail(emailRequest: SendEmailRequest): Future[Boolean] = {
    implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(CONTENT_TYPE -> "application/json")

    postHttpRequest(emailRequest).map {
      case Left(UpstreamErrorResponse(_, statusCode, _, _)) =>
        logger.warn(s"Error while sending an email for templateId ${emailRequest.templateId}: $statusCode")
        false
      case Right(_)                                         =>
        true
    }
      .recoverWith {
        case NonFatal(e) =>
          logger.warn(s"NonFatal error ${e.getMessage} while sending message for templateId ${emailRequest.templateId}", e)
          Future.successful(false)
      }
  }

  private def postHttpRequest(request: SendEmailRequest)(implicit hc: HeaderCarrier): Future[Either[Throwable, Boolean]] = {
    val oneEmailRequest = OneEmailRequest(List(request.to), request.templateId, request.parameters, request.force, request.auditData, request.eventUrl, request.tags)
    http.POST[OneEmailRequest, HttpResponse](s"$serviceUrl/developer/email", oneEmailRequest).map {
      res => Right(res.status == Status.ACCEPTED)
    }
      .recover {
        case NonFatal(e) =>
          logger.error(e.getMessage)
          Left(e)
      }
  }
}
