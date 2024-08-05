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

import play.api.Logging
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, StringContextOps, UpstreamErrorResponse}

import uk.gov.hmrc.gatekeeperemail.config.EmailRendererConnectorConfig
import uk.gov.hmrc.gatekeeperemail.models.requests.{DraftEmailRequest, TemplateRenderRequest}
import uk.gov.hmrc.gatekeeperemail.models.{RenderResult, TemplateRenderResult}

@Singleton
class GatekeeperEmailRendererConnector @Inject() (httpClient: HttpClientV2, config: EmailRendererConnectorConfig)(implicit ec: ExecutionContext)
    extends HttpErrorFunctions with Logging {

  private lazy val serviceUrl = config.emailRendererBaseUrl

  def getTemplatedEmail(emailRequest: DraftEmailRequest): Future[Either[UpstreamErrorResponse, RenderResult]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(CONTENT_TYPE -> "application/json")

    httpClient.post(url"$serviceUrl/templates/${emailRequest.templateId}")
      .withBody(Json.toJson(TemplateRenderRequest(emailRequest.parameters, None)))
      .execute[TemplateRenderResult]
      .map { result =>
        Right(
          RenderResult(
            result.plain,
            result.html,
            result.fromAddress,
            result.subject,
            result.service
          )
        )
      } recover {
      case errorResponse: UpstreamErrorResponse =>
        Left(errorResponse)
    }
  }
}
