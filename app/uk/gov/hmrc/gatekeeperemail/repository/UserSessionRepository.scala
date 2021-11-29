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

import javax.inject.Inject
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.gatekeeperemail.connectors.Reference
import uk.gov.hmrc.gatekeeperemail.model.{Failed, InProgress, UploadId, UploadStatus, UploadedFailedWithErrors, UploadedSuccessfully}
import uk.gov.hmrc.gatekeeperemail.repository.UploadDetails._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.mongoEntity

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class UploadDetails(id : BSONObjectID, uploadId : UploadId, reference : Reference, status : UploadStatus)

object UploadDetails {
  val status = "status"

  implicit val uploadedSuccessfullyFormat: OFormat[UploadedSuccessfully] = Json.format[UploadedSuccessfully]
  implicit val uploadedFailed: OFormat[UploadedFailedWithErrors] = Json.format[UploadedFailedWithErrors]

  implicit val read: Reads[UploadStatus] = new Reads[UploadStatus] {
    override def reads(json: JsValue): JsResult[UploadStatus] = {
      val jsObject = json.asInstanceOf[JsObject]
      jsObject.value.get("_type") match {
        case Some(JsString("InProgress")) => JsSuccess(InProgress)
        case Some(JsString("Failed")) => JsSuccess(Failed)
        case Some(JsString("UploadedSuccessfully")) => Json.fromJson[UploadedSuccessfully](jsObject)(uploadedSuccessfullyFormat)
        case Some(JsString("UploadedFailedWithErrors")) => Json.fromJson[UploadedFailedWithErrors](jsObject)(uploadedFailed)
        case Some(value) => JsError(s"Unexpected value of _type: $value")
        case None => JsError("Missing _type field")
      }
    }
  }

  val write: Writes[UploadStatus] = new Writes[UploadStatus] {
    override def writes(p: UploadStatus): JsValue = {
      p match {
        case InProgress => JsObject(Map("_type" -> JsString("InProgress")))
        case Failed => JsObject(Map("_type" -> JsString("Failed")))
        case s : UploadedSuccessfully => Json.toJson(s)(uploadedSuccessfullyFormat).as[JsObject] + ("_type" -> JsString("UploadedSuccessfully"))
        case f : UploadedFailedWithErrors => Json.toJson(f)(uploadedFailed).as[JsObject] + ("_type" -> JsString("uploadedFailed"))
      }
    }
  }

  implicit val uploadStatusFormat: Format[UploadStatus] = Format(read,write)

  implicit val idFormat: OFormat[UploadId] = Json.format[UploadId]

  implicit val referenceFormat: OFormat[Reference] = Json.format[Reference]

  val format: Format[UploadDetails] = mongoEntity ( Json.format[UploadDetails] )
}


class UserSessionRepository @Inject() (mongoComponent: ReactiveMongoComponent)(implicit ec : ExecutionContext)
  extends ReactiveRepository[UploadDetails, BSONObjectID](
  collectionName = "gatekeeperfileuploads",
  mongo = mongoComponent.mongoConnector.db,
  domainFormat = UploadDetails.format,
  idFormat = ReactiveMongoFormats.objectIdFormats
) {

  def findByUploadId(uploadId: UploadId): Future[Option[UploadDetails]] =
    find("uploadId" -> Json.toJson(uploadId)).map(_.headOption)

  def updateStatus(reference : Reference, newStatus : UploadStatus): Future[UploadStatus] =
    for (result <- findAndUpdate(
      query = JsObject(Seq("reference" -> Json.toJson(reference))),
      update = Json.obj(
        "$set" -> Json.obj(
          status -> Json.toJson(newStatus)
        )
      ), upsert = true)) yield {
      result.result[UploadDetails].map(_.status).getOrElse(throw new Exception("Update failed, no document modified"))
    }


}
