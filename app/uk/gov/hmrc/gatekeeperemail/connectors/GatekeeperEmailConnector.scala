/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.gatekeeperemail.config.EmailConnectorConfig
import uk.gov.hmrc.gatekeeperemail.models.{OneEmailRequest, SendEmailRequest}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpErrorFunctions, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GatekeeperEmailConnector @Inject()(http: HttpClient, config: EmailConnectorConfig)(implicit ec: ExecutionContext)
  extends HttpErrorFunctions with Logging {

  lazy val serviceUrl = config.emailBaseUrl

 def sendEmail(emailRequest: SendEmailRequest): Future[Int] = {
    implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(CONTENT_TYPE -> "application/json")
    postHttpRequest(emailRequest)
  }

 private def postHttpRequest(request: SendEmailRequest)(implicit hc: HeaderCarrier): Future[Int] = {
    val oneEmailRequest = OneEmailRequest(List(request.to), request.templateId, request.parameters, request.force,
      request.auditData, request.eventUrl, request.tags)
    http.POST[OneEmailRequest, HttpResponse](s"$serviceUrl/developer/email",
      oneEmailRequest) map { response => response.status }
  }
}
