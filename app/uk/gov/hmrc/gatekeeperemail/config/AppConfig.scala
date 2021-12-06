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

package uk.gov.hmrc.gatekeeperemail.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject()(config: Configuration,
                          servicesConfig: ServicesConfig)
  extends ServicesConfig(config)
    with EmailConnectorConfig
{

  val authBaseUrl: String = baseUrl("auth")
  val emailBaseUrl: String = baseUrl("email")

  val auditingEnabled: Boolean = config.get[Boolean]("auditing.enabled")
  val graphiteHost: String = config.get[String]("microservice.metrics.graphite.host")


  lazy val assetsPrefix   = loadConfig(s"assets.url") + loadConfig(s"assets.version")
  lazy val analyticsToken = loadConfig(s"google-analytics.token")
  lazy val analyticsHost  = loadConfig(s"google-analytics.host")

  lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl   = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  private val contactHost                  = config.getOptional[String](s"contact-frontend.host").getOrElse("")
  private val contactFormServiceIdentifier = "MyService"

  private def loadConfig(key: String) =
    config.getOptional[String](key).getOrElse(throw new Exception(s"Missing configuration key: $key"))
}

trait EmailConnectorConfig {
  val emailBaseUrl: String
}

