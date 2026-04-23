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

import play.api.libs.json._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.services.SealedTraitJsonFormatting
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

@Singleton
class ApmConnector @Inject() (http: HttpClientV2, config: ApmConnector.Config)(implicit ec: ExecutionContext) {

  private def mapText(in: String): Option[ApiAccessType] = ApiAccessType.apply(in).orElse(Some(ApiAccessType.INTERNAL))

  private implicit val overrideFormatForApiAccessType: Format[ApiAccessType] = SealedTraitJsonFormatting.createFormatFor[ApiAccessType]("API Access Type", mapText)

  private implicit val overrideFormatCombinedApi: OFormat[CombinedApi] = Json.format[CombinedApi]

  def fetchAllCombinedApis()(implicit hc: HeaderCarrier): Future[List[CombinedApi]] = {
    http.get(url"${config.serviceBaseUrl}/combined-rest-xml-apis").execute[List[CombinedApi]]
  }

}

object ApmConnector {

  case class Config(
      serviceBaseUrl: String
    )
}
