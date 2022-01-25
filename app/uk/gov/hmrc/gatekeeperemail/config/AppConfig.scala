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

package uk.gov.hmrc.gatekeeperemail.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject()(config: Configuration)
  extends ServicesConfig(config)
    with EmailConnectorConfig
    with EmailRendererConnectorConfig
{

  val authBaseUrl: String = baseUrl("auth")
  val emailBaseUrl: String = baseUrl("email")
  val emailRendererBaseUrl: String = baseUrl("developer-email-renderer")
  val defaultRetentionPeriod: String = getConfString("object-store.default-retention-period", "1-year")
  val auditingEnabled: Boolean = config.get[Boolean]("auditing.enabled")
  val graphiteHost: String = config.get[String]("microservice.metrics.graphite.host")
}

trait EmailConnectorConfig {
  val emailBaseUrl: String
}

trait EmailRendererConnectorConfig {
  val emailRendererBaseUrl: String
}