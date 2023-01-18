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

import com.google.inject.name.Named

import play.api.Logging
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.models.TopicOptionChoice.TopicOptionChoice
import uk.gov.hmrc.gatekeeperemail.models._

trait DeveloperConnector {

  def fetchAll()(implicit hc: HeaderCarrier): Future[List[RegisteredUser]]

  def fetchByEmailPreferences(
      topic: TopicOptionChoice,
      maybeApis: Option[Seq[String]] = None,
      maybeApiCategory: Option[Seq[APICategory]] = None,
      privateapimatch: Boolean = false
    )(implicit hc: HeaderCarrier
    ): Future[List[RegisteredUser]]
}

@Singleton
class HttpDeveloperConnector @Inject() (appConfig: AppConfig, http: HttpClient)(implicit ec: ExecutionContext)
    extends DeveloperConnector with Logging {

  def fetchByEmailPreferences(
      topic: TopicOptionChoice,
      maybeApis: Option[Seq[String]] = None,
      maybeApiCategories: Option[Seq[APICategory]] = None,
      privateapimatch: Boolean = false
    )(implicit hc: HeaderCarrier
    ): Future[List[RegisteredUser]] = {
    logger.info(s"fetchByEmailPreferences topic is $topic maybeApis: $maybeApis maybeApuCategories $maybeApiCategories privateapimatch $privateapimatch")
    val regimes: Seq[(String, String)] = maybeApiCategories.fold(Seq.empty[(String, String)])(regimes =>
      regimes.flatMap(regime => Seq("regime" -> regime.value))
    )
    val privateapimatchParams          = if (privateapimatch) Seq("privateapimatch" -> "true") else Seq.empty
    val queryParams                    =
      Seq("topic" -> topic.toString) ++ regimes ++
        maybeApis.fold(Seq.empty[(String, String)])(apis => apis.map(("service" -> _))) ++ privateapimatchParams

    http.GET[List[RegisteredUser]](s"${appConfig.developerBaseUrl}/developers/email-preferences", queryParams)
  }

  def fetchAll()(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = {
    http.GET[List[RegisteredUser]](s"${appConfig.developerBaseUrl}/developers/all")
  }

}
