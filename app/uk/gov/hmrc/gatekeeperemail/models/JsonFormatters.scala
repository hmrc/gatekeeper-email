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

package uk.gov.hmrc.gatekeeperemail.models

import play.api.libs.json._

import uk.gov.hmrc.gatekeeperemail.repositories.UploadInfo

trait JsonFormatters {

  implicit val bsonFormat: OFormat[UploadId] = Json.format[UploadId]

  implicit val referenceFormat: OFormat[Reference] = Json.format[Reference]

  // NOTE - these override the defaults in order to push dates in non-mongo format
  implicit val uploadedSuccessfullyFormat: OFormat[UploadedSuccessfully]         = Json.format[UploadedSuccessfully]
  implicit val uploadedFailedWithErrorsFormat: OFormat[UploadedFailedWithErrors] = Json.format[UploadedFailedWithErrors]

  implicit val read: Reads[UploadStatus] = new Reads[UploadStatus] {

    override def reads(json: JsValue): JsResult[UploadStatus] = {
      val jsObject = json.asInstanceOf[JsObject]
      jsObject.value.get("_type") match {
        case Some(JsString("InProgress"))               => JsSuccess(InProgress)
        case Some(JsString("Failed"))                   => JsSuccess(Failed)
        case Some(JsString("UploadedSuccessfully"))     => Json.fromJson[UploadedSuccessfully](jsObject)(uploadedSuccessfullyFormat)
        case Some(JsString("UploadedFailedWithErrors")) => Json.fromJson[UploadedFailedWithErrors](jsObject)(uploadedFailedWithErrorsFormat)
        case Some(value)                                => JsError(s"Unexpected value of _type: $value")
        case None                                       => JsError("Missing _type field")
      }
    }
  }

  val write: Writes[UploadStatus] = new Writes[UploadStatus] {

    override def writes(p: UploadStatus): JsObject = {
      p match {
        case InProgress                  => JsObject(Map("_type" -> JsString("InProgress")))
        case Failed                      => JsObject(Map("_type" -> JsString("Failed")))
        case BadRequest                  => JsObject(Map("_type" -> JsString("BadRequest")))
        case s: UploadedSuccessfully     => uploadedSuccessfullyFormat.writes(s) ++ Json.obj("_type" -> "UploadedSuccessfully")
        case f: UploadedFailedWithErrors => uploadedFailedWithErrorsFormat.writes(f) ++ Json.obj("_type" -> "uploadedFailed")
      }
    }
  }

  implicit val uploadStatusFormat: Format[UploadStatus] = Format(read, write)
  val uploadInfoReads                                   = Json.reads[UploadInfo]
  val uploadInfoWrites                                  = Json.writes[UploadInfo]
  implicit val uploadInfoFormat: Format[UploadInfo]     = Format(uploadInfoReads, uploadInfoWrites)
}

object JsonFormatters extends JsonFormatters
