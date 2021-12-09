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

package uk.gov.hmrc.gatekeeperemail.services

import uk.gov.hmrc.gatekeeperemail.models.{InProgress, Reference, UploadId, UploadStatus}
import uk.gov.hmrc.gatekeeperemail.repository.{FileUploadStatusRepository, UploadInfo}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileUploadStatusService @Inject()(repository : FileUploadStatusRepository)(implicit ec : ExecutionContext) extends UploadProgressTracker {

  override def requestUpload(uploadId : UploadId, fileReference : Reference): Future[Unit] =
    repository.requestUpload(UploadInfo(uploadId, fileReference, InProgress)).map(_ => ())

  override def registerUploadResult(fileReference: Reference, uploadStatus: UploadStatus): Future[Unit] =
    repository.updateStatus(fileReference, uploadStatus).map(_ => ())

  override def getUploadResult(key: Reference): Future[Option[UploadInfo]] =
    for (result <- repository.findByUploadId(key)) yield {
      result
    }
}
