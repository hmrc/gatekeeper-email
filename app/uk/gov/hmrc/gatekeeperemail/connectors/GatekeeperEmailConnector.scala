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

import uk.gov.hmrc.gatekeeperemail.config.EmailConnectorConfig
import uk.gov.hmrc.gatekeeperemail.models.SendEmailRequest.createEmailRequest
import uk.gov.hmrc.gatekeeperemail.models.SendEmailRequest
import uk.gov.hmrc.gatekeeperemail.util.ApplicationLogger
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.http.metrics.common.API

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GatekeeperEmailConnector @Inject()(http: HttpClient, config: EmailConnectorConfig)(implicit ec: ExecutionContext)
  extends CommonResponseHandlers
  with ApplicationLogger {

  val api = API("email")
  lazy val serviceUrl = config.emailBaseUrl

  def sendEmail(request: SendEmailRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    logger.info(s"*****sendEmailTo*********:${request.to}")
    post(request)
  }

  private def post(request: SendEmailRequest)(implicit hc: HeaderCarrier) = {
    logger.info(s"*******sendEmailRequest:$request")
    http.POST[SendEmailRequest, ErrorOrUnit](s"$serviceUrl/developer/email", request)
    .map(throwOrUnit)
  }
}
