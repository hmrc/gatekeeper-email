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

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import play.api.http.Status.{CONFLICT, NOT_FOUND}
import play.api.libs.json.Json
import uk.gov.hmrc.gatekeeperemail.models.CombinedApi
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApmConnector @Inject() (http: HttpClient, config: ApmConnector.Config)(implicit ec: ExecutionContext) {


  def fetchAllCombinedApis()(implicit hc: HeaderCarrier): Future[List[CombinedApi]] = {
    http.GET[List[CombinedApi]](s"${config.serviceBaseUrl}/combined-rest-xml-apis")
  }


}

object ApmConnector {

  case class Config(
                     serviceBaseUrl: String
                   )
}
