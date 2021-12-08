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

package uk.gov.hmrc.gatekeeperemail.connectors

import org.apache.commons.io.Charsets
import org.apache.http.HttpStatus
import play.api.Logging
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.gatekeeperemail.config.EmailConnectorConfig
import uk.gov.hmrc.gatekeeperemail.models.{EmailData, EmailRequest, ErrorMessage, RenderResult, SendEmailRequest, TemplateRenderRequest, TemplateRenderResult}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpClient, HttpErrorFunctions, HttpResponse, NotFoundException, UpstreamErrorResponse}
import uk.gov.hmrc.play.http.metrics.common.API

import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GatekeeperEmailRendererConnector @Inject()(httpClient: HttpClient, config: EmailConnectorConfig)(implicit ec: ExecutionContext)
  extends HttpErrorFunctions with Logging {

  val api = API("email")
  lazy val serviceUrl = config.emailBaseUrl


  def getTemplatedEmail(emailRequest: SendEmailRequest): Future[Either[UpstreamErrorResponse, RenderResult]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(CONTENT_TYPE -> "application/json")

    def base64Decode(result: String): String =
      new String(Base64.getDecoder.decode(result), Charsets.UTF_8)

    httpClient.POST[TemplateRenderRequest, TemplateRenderResult](
      s"$serviceUrl/templates/${emailRequest.templateId}",
      TemplateRenderRequest(emailRequest.parameters, takeOnlyIfOneEmail(emailRequest.to))) map { result =>
      Right(
          RenderResult(
          base64Decode(result.plain),
          base64Decode(result.html),
          result.fromAddress,
          result.subject,
          result.service))
    } recover {
      case _: NotFoundException =>
        Left(UpstreamErrorResponse(s"Template ${emailRequest.templateId} does not exist", HttpStatus.SC_NOT_FOUND))
      case ex: BadRequestException =>
        Left(UpstreamErrorResponse(ex.getMessage, HttpStatus.SC_BAD_REQUEST))
    }
  }

  def takeOnlyIfOneEmail(emails: List[String]): Option[String] =
    emails match {
      case first :: Nil => Some(first)
      case _            => None
    }

}
