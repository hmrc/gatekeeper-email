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

import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.controllers.{CallbackBody, FailedCallbackBody, ReadyCallbackBody}
import uk.gov.hmrc.gatekeeperemail.models._
import uk.gov.hmrc.gatekeeperemail.repositories.{FileUploadStatusRepository, UploadInfo}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.objectstore.client.{Path, RetentionPeriod}
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient

import java.net.URL
import javax.inject.Inject
import scala.concurrent.Future

class UpscanCallbackService @Inject()(sessionStorage: FileUploadStatusRepository,
                                      objectStoreClient: PlayObjectStoreClient,
                                      appConfig: AppConfig
                                     ) {

  def handleCallback(callback : CallbackBody): Future[UploadInfo] = {
    implicit val hc = HeaderCarrier()

    def uploadToObjectStore(readyCallback: ReadyCallbackBody) = {
      objectStoreClient.uploadFromUrl(from = new URL(readyCallback.downloadUrl),
        to = Path.File(Path.Directory("gatekeeper-email"), readyCallback.uploadDetails.fileName),
        retentionPeriod = RetentionPeriod.parse(appConfig.defaultRetentionPeriod).getOrElse(RetentionPeriod.OneYear),
        owner = "gatekeeper"
      )
    }

    val uploadStatus = callback match {
      case s: ReadyCallbackBody =>
        val uploaded = UploadedSuccessfully(
          s.uploadDetails.fileName,
          s.uploadDetails.fileMimeType,
          s.downloadUrl,
          Some(s.uploadDetails.size)
        )
        uploadToObjectStore(s)
        uploaded
      case f: FailedCallbackBody =>
        UploadedFailedWithErrors(f.fileStatus, f.failureDetails.failureReason, f.failureDetails.message, f.reference)
      case _ => Failed
    }
    sessionStorage.updateStatus(Reference(callback.reference), uploadStatus)
  }

}
