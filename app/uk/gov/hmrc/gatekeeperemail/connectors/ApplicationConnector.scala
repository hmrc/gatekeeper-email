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

import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.models.{ApiContext, ApiVersion}
import uk.gov.hmrc.http.{HttpClient, _}

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.{failed, successful}
import scala.concurrent.{ExecutionContext, Future}

object ApplicationConnector {
  import play.api.libs.json.Json

  case class ValidateApplicationNameResponseErrorDetails(invalidName: Boolean, duplicateName: Boolean)
  case class ValidateApplicationNameResponse(errors: Option[ValidateApplicationNameResponseErrorDetails])

  implicit val validateApplicationNameResponseErrorDetailsReads = Json.reads[ValidateApplicationNameResponseErrorDetails]
  implicit val validateApplicationNameResponseReads = Json.reads[ValidateApplicationNameResponse]

  case class SearchCollaboratorsRequest(apiContext: ApiContext, apiVersion: ApiVersion, partialEmailMatch: Option[String])

  implicit val writes = Json.writes[SearchCollaboratorsRequest]
}
case class ApplicationId(value: String) extends AnyVal

object ApplicationId {
  import play.api.libs.json.Json
  implicit val applicationIdFormat = Json.valueFormat[ApplicationId]

  def random: ApplicationId = ApplicationId(UUID.randomUUID().toString)
}

object Environment extends Enumeration {
  type Environment = Value
  val SANDBOX, PRODUCTION = Value
  implicit val format = Json.formatEnum(Environment)

  implicit class Display(e: Environment) {
    def asDisplayed() = e match {
      case SANDBOX => "Sandbox"
      case PRODUCTION => "Production"
    }
  }
}

abstract class ApplicationConnector(implicit val ec: ExecutionContext)  {
  import ApplicationConnector._

  protected val httpClient: HttpClient
  val environment: Environment.Environment

  val serviceBaseUrl: String

  def http: HttpClient

  def baseApplicationUrl(applicationId: ApplicationId) = s"$serviceBaseUrl/application/${applicationId.value}"
  
  import uk.gov.hmrc.http.HttpReads.Implicits._



  def searchCollaborators(apiContext: ApiContext, apiVersion: ApiVersion, partialEmailMatch: Option[String])(implicit hc: HeaderCarrier): Future[List[String]] = {
    val request = SearchCollaboratorsRequest(apiContext, apiVersion, partialEmailMatch)

    http.POST[SearchCollaboratorsRequest,List[String]](s"$serviceBaseUrl/collaborators", request)
  }


}

@Singleton
class SandboxApplicationConnector @Inject()(val appConfig: AppConfig,
                                            val httpClient: HttpClient,
                                            val proxiedHttpClient: ProxiedHttpClient)(implicit override val ec: ExecutionContext)
  extends ApplicationConnector {

  val environment = Environment.SANDBOX
  val serviceBaseUrl = appConfig.applicationSandboxBaseUrl
  val useProxy = appConfig.applicationSandboxUseProxy
  val bearerToken = appConfig.applicationSandboxBearerToken
  val apiKey = appConfig.applicationSandboxApiKey

  val http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient

}

@Singleton
class ProductionApplicationConnector @Inject()(val appConfig: AppConfig,
                                               val httpClient: HttpClient)(implicit override val ec: ExecutionContext)
  extends ApplicationConnector {

  val environment = Environment.PRODUCTION
  val serviceBaseUrl = appConfig.applicationProductionBaseUrl

  val http = httpClient
}
