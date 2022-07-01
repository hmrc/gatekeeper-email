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

package uk.gov.hmrc.gatekeeperemail.models

import java.util.UUID

sealed trait UploadStatus
case object InProgress extends UploadStatus
case object Failed extends UploadStatus
case object BadRequest extends UploadStatus
case class UploadedSuccessfully(name: String, mimeType: String, downloadUrl: String, size: Option[Long], objectStoreUrl: String) extends UploadStatus
case class UploadedFailedWithErrors(errorCode: String, errorMessage: String, errorRequestId: String, key: String) extends UploadStatus

case class UploadId(value : UUID) extends AnyVal

case class Reference(value: String) extends AnyVal

case class User (email: String, firstName: String, lastName: String,
                 verified: Boolean)

