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

package uk.gov.hmrc.gatekeeperemail.services

import play.api.Logger
import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.objectstore.client.{Path, RetentionPeriod}
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ObjectStoreService @Inject()(objectStoreClient: PlayObjectStoreClient,
                                    appConfig: AppConfig)
                                  (implicit val ec: ExecutionContext) {

  val logger: Logger = Logger(getClass.getName)

  def uploadToObjectStore(emailUUID: String, downloadUrl: String, fileName: String) = {
    implicit val hc = HeaderCarrier()
    logger.info(s"uploadToObjectStore upload to location: $emailUUID")
    objectStoreClient.uploadFromUrl(from = new URL(downloadUrl),
      to = Path.File(Path.Directory(emailUUID), fileName),
      retentionPeriod = RetentionPeriod.parse(appConfig.defaultRetentionPeriod).getOrElse(RetentionPeriod.OneYear),
      contentType = None,
      contentMd5 = None,
      owner = "gatekeeper-email"
    )
  }

  def deleteFromObjectStore(emailUUID: String, fileName: String) = {
    implicit val hc = HeaderCarrier()
    logger.info(s"deleteFromObjectStore emailUUID = $emailUUID")
    objectStoreClient.deleteObject(
      path = Path.File(Path.Directory(emailUUID), fileName),
      owner = "gatekeeper-email"
    )
  }

}
