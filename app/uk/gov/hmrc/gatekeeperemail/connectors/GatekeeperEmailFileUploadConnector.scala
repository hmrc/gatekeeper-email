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

import play.api.Logging
import uk.gov.hmrc.gatekeeperemail.config.EmailConnectorConfig
import uk.gov.hmrc.gatekeeperemail.repositories.UploadInfo
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpErrorFunctions}
import uk.gov.hmrc.play.http.metrics.common.API

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GatekeeperEmailFileUploadConnector @Inject()(http: HttpClient, config: EmailConnectorConfig)(implicit ec: ExecutionContext)
  extends HttpErrorFunctions with Logging {
  val api = API("email")
  lazy val serviceUrl = config.emailBaseUrl
  def inProgressUploadStatus(keyReference: String)(implicit hc: HeaderCarrier): Future[UploadInfo] = {
    http.POSTEmpty[UploadInfo](s"$serviceUrl/gatekeeperemail/insertfileuploadstatus?key=$keyReference")
  }

  def fetchFileuploadStatus(key: String)(implicit hc: HeaderCarrier) = {
    val url = s"$serviceUrl/gatekeeperemail/fetchfileuploadstatus"
    http.GET[UploadInfo](url, Seq(("key", key)))
  }
}
