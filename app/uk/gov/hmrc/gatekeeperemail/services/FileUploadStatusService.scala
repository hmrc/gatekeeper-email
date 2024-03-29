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

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.apiplatform.modules.common.services.ClockNow

import uk.gov.hmrc.gatekeeperemail.models.{InProgress, Reference, UploadStatus}
import uk.gov.hmrc.gatekeeperemail.repositories.{FileUploadStatusRepository, UploadInfo}

class FileUploadStatusService @Inject() (repository: FileUploadStatusRepository, val clock: Clock)(implicit ec: ExecutionContext) extends UploadProgressTracker with ClockNow {

  override def requestUpload(fileReference: String): Future[UploadInfo] =
    repository.requestUpload(UploadInfo(Reference(fileReference), InProgress, instant()))

  override def registerUploadResult(fileReference: String, uploadStatus: UploadStatus): Future[UploadInfo] =
    repository.updateStatus(Reference(fileReference), uploadStatus)

  override def getUploadResult(key: Reference): Future[Option[UploadInfo]] =
    for (result <- repository.findByUploadId(key)) yield {
      result
    }
}
