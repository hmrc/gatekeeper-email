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
import uk.gov.hmrc.gatekeeperemail.config.EmailRendererConnectorConfig
import uk.gov.hmrc.gatekeeperemail.models.{DraftEmailRequest, RenderResult, TemplateRenderRequest, TemplateRenderResult}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpErrorFunctions, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GatekeeperEmailRendererConnector @Inject()(httpClient: HttpClient, config: EmailRendererConnectorConfig)(implicit ec: ExecutionContext)
  extends HttpErrorFunctions with Logging {

  private lazy val serviceUrl = config.emailRendererBaseUrl

  def getTemplatedEmail(emailRequest: DraftEmailRequest): Future[Either[UpstreamErrorResponse, RenderResult]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(CONTENT_TYPE -> "application/json")

    httpClient.POST[TemplateRenderRequest, TemplateRenderResult](
      s"$serviceUrl/templates/${emailRequest.templateId}",
      TemplateRenderRequest(emailRequest.parameters, None)) map { result =>
      Right(
          RenderResult(
          result.plain,
          result.html,
          result.fromAddress,
          result.subject,
          result.service))
    } recover {
      case errorResponse: UpstreamErrorResponse =>
        Left(errorResponse)
    }
  }
}
