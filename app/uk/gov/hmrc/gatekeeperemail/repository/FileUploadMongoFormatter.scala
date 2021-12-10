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

package uk.gov.hmrc.gatekeeperemail.repository

import play.api.libs.json._
import uk.gov.hmrc.gatekeeperemail.models._

object FileUploadMongoFormatter {
  implicit val bsonFormat: OFormat[UploadId] = Json.format[UploadId]

  implicit val referenceFormat: OFormat[Reference] = Json.format[Reference]

  // NOTE - these override the defaults in order to push dates in non-mongo format
  //implicit val uploadedSuccessfullyFormat: OFormat[UploadedSuccessfully] = Json.format[UploadedSuccessfully]

  implicit val initiateReads: Reads[InProgress.type] =
    Json.reads[InProgress.type ]
  implicit val initiateWrites: OWrites[InProgress.type] =
    Json.writes[InProgress.type ].transform(_ ++ Json.obj("_type" -> "InProgress"))
  implicit val initiateFormat: OFormat[InProgress.type] =
    OFormat(initiateReads, initiateWrites)

  implicit val failedReads: Reads[Failed.type] =
    Json.reads[Failed.type ]
  implicit val failedWrites: OWrites[Failed.type] =
    Json.writes[Failed.type ].transform(_ ++ Json.obj("_type" -> "Failed"))
  implicit val failedFormat: OFormat[Failed.type] =
    OFormat(failedReads, failedWrites)

  implicit val uploadedSuccessfullyReads: Reads[UploadedSuccessfully] =
    Json.reads[UploadedSuccessfully]
  implicit val uploadedSuccessfullyWrites: OWrites[UploadedSuccessfully] =
    Json.writes[UploadedSuccessfully].transform(_ ++ Json.obj("_type" -> "UploadedSuccessfully"))
  implicit val uploadedSuccessfullyFormat: OFormat[UploadedSuccessfully] =
    OFormat(uploadedSuccessfullyReads, uploadedSuccessfullyWrites)
//  implicit val uploadedFailed: OFormat[UploadedFailedWithErrors] = Json.format[UploadedFailedWithErrors]
  implicit val uploadedFailedReads: Reads[UploadedFailedWithErrors] =
    Json.reads[UploadedFailedWithErrors]
  implicit val uploadedFailedWrites: OWrites[UploadedFailedWithErrors] =
    Json.writes[UploadedFailedWithErrors].transform(_ ++ Json.obj("_type" -> "UploadedFailedWithErrors"))
  implicit val uploadedFailedFormat: OFormat[UploadedFailedWithErrors] =
    OFormat(uploadedFailedReads, uploadedFailedWrites)

  implicit val read: Reads[UploadStatus] = new Reads[UploadStatus] {
    override def reads(json: JsValue): JsResult[UploadStatus] = {
      val jsObject = json.asInstanceOf[JsObject]
      jsObject.value.get("_type") match {
        case Some(JsString("InProgress")) => JsSuccess(InProgress)
        case Some(JsString("Failed")) => JsSuccess(Failed)
        case Some(JsString("UploadedSuccessfully")) => Json.fromJson[UploadedSuccessfully](jsObject)(uploadedSuccessfullyFormat)
        case Some(JsString("UploadedFailedWithErrors")) => Json.fromJson[UploadedFailedWithErrors](jsObject)(uploadedFailedFormat)
      }
    }
  }

  val write: Writes[UploadStatus] = new Writes[UploadStatus] {
    override def writes(p: UploadStatus): JsObject = {
      p match {
        case InProgress => JsObject(Map("_type" -> JsString("InProgress")))
        case Failed => JsObject(Map("_type" -> JsString("Failed")))
        case s : UploadedSuccessfully => {
          val result = uploadedSuccessfullyFormat.writes(s) ++ Json.obj("_type" -> "UploadedSuccessfully")
          println(s"********* $result ********")
          result
        }
        case f : UploadedFailedWithErrors => uploadedFailedFormat.writes(f) ++ Json.obj("_type" -> "UploadedFailedWithErrors")
      }
    }
  }
  implicit val uploadStatusFormat: Format[UploadStatus] = Format(read,write)
  val uploadInfoReads = Json.reads[UploadInfo]
  val uploadInfoWrites = Json.writes[UploadInfo]
  implicit val uploadInfo = Json.format[UploadInfo]
}
