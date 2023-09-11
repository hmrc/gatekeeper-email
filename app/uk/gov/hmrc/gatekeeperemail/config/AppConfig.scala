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

package uk.gov.hmrc.gatekeeperemail.config

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, FiniteDuration}

import play.api.{Configuration, Logging}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import uk.gov.hmrc.gatekeeperemail.config.AdditionalRecipientsConfigProvider.configLoader

@Singleton
class AppConfig @Inject() (config: Configuration)
    extends ServicesConfig(config)
    with EmailConnectorConfig
    with EmailRendererConnectorConfig with Logging {

  val authBaseUrl: String          = baseUrl("auth")
  val emailBaseUrl: String         = baseUrl("email")
  val emailRendererBaseUrl: String = baseUrl("developer-email-renderer")
  val additionalRecipients         = config.getOptional[List[AdditionalRecipient]]("additionalRecipients").getOrElse(List())
  val sendToActualRecipients       = config.get[Boolean]("sendToActualRecipients")

  val emailRecordRetentionPeriod: Int = config.get[Int]("mongodb.ttlInYears")
  val defaultRetentionPeriod: String  = config.get[String]("object-store.default-retention-period")
  val auditingEnabled: Boolean        = config.get[Boolean]("auditing.enabled")

  val developerBaseUrl = baseUrl("third-party-developer")

  val graphiteHost: String = config.get[String]("microservice.metrics.graphite.host")

  def scheduledJobBatchSize(jobName: String) = config.get[Int](s"scheduledJobs.$jobName.batchSize")

  def scheduledJobConfig(jobName: String) = ScheduledJobConfig(
    Duration(config.get[String](s"scheduledJobs.$jobName.initialDelay")).asInstanceOf[FiniteDuration],
    Duration(config.get[String](s"scheduledJobs.$jobName.interval")).asInstanceOf[FiniteDuration],
    config.get[Boolean](s"scheduledJobs.$jobName.enabled")
  )
}

case class ScheduledJobConfig(initialDelay: FiniteDuration, interval: FiniteDuration, enabled: Boolean)

trait EmailConnectorConfig {
  val emailBaseUrl: String
}

trait EmailRendererConnectorConfig {
  val emailRendererBaseUrl: String
}
