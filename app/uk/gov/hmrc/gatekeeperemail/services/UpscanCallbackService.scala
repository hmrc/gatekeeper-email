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

package uk.gov.hmrc.gatekeeperemail.services

import play.api.Logger
import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.controllers.{CallbackBody, FailedCallbackBody, ReadyCallbackBody}
import uk.gov.hmrc.gatekeeperemail.models._
import uk.gov.hmrc.gatekeeperemail.repositories.{FileUploadStatusRepository, UploadInfo}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.objectstore.client.{ObjectSummaryWithMd5, Path, RetentionPeriod}
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpscanCallbackService @Inject()(sessionStorage: FileUploadStatusRepository,
                                      objectStoreClient: PlayObjectStoreClient,
                                      appConfig: AppConfig
                                     )(implicit val ec: ExecutionContext) {

  private val logger = Logger(this.getClass)
  implicit val hc = HeaderCarrier()

  def uploadToObjectStore(readyCallback: ReadyCallbackBody) = {
    logger.info(s"uploadToObjectStore $readyCallback")
    objectStoreClient.uploadFromUrl(from = new URL(readyCallback.downloadUrl),
      to = Path.File(Path.Directory("gatekeeper-email"), readyCallback.uploadDetails.fileName),
      retentionPeriod = RetentionPeriod.parse(appConfig.defaultRetentionPeriod).getOrElse(RetentionPeriod.OneYear),
      contentType = None,
      contentMd5 = None,
      owner = "gatekeeper-email"
    )
  }

  //TODO add UDF object store logic here..
  def handleCallback(callback : CallbackBody): Future[UploadInfo] = {

    callback match {
      case s: ReadyCallbackBody =>
        //val objectSummary: Future[ObjectSummaryWithMd5] = uploadToObjectStore(s)
        //objectSummary.flatMap { summary =>
          val status = UploadedSuccessfully(
          s.uploadDetails.fileName,
          s.uploadDetails.fileMimeType,
          s.downloadUrl,
          Some(s.uploadDetails.size),
          "summary.location.asUri")
          sessionStorage.updateStatus((callback.reference), status)
        //TODO add in object store..
      case f: FailedCallbackBody =>
        val status = UploadedFailedWithErrors(f.fileStatus, f.failureDetails.failureReason, f.failureDetails.message, f.reference)
        sessionStorage.updateStatus((callback.reference), status)
      case _ =>
        val status = Failed
        sessionStorage.updateStatus((callback.reference), status)
    }
  }

}
