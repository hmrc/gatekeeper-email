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

import com.google.inject.ImplementedBy
import org.mongodb.scala.result.InsertOneResult
import uk.gov.hmrc.gatekeeperemail.models.{Reference, UploadStatus}
import uk.gov.hmrc.gatekeeperemail.repositories.UploadInfo

import scala.concurrent.Future


@ImplementedBy(classOf[FileUploadStatusService])
trait UploadProgressTracker {

  def requestUpload(fileReference : String) : Future[UploadInfo]

  def registerUploadResult(reference : String, uploadStatus : UploadStatus): Future[UploadInfo]

  def getUploadResult(id : Reference): Future[Option[UploadInfo]]

}
