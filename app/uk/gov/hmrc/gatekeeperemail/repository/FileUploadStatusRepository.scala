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
import uk.gov.hmrc.gatekeeperemail.models.{Reference, UploadId, UploadStatus}
import uk.gov.hmrc.gatekeeperemail.repository.UploadInfo._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.mongoEntity
import uk.gov.hmrc.gatekeeperemail.models.JsonFormatters._
import uk.gov.hmrc.gatekeeperemail.controllers.UploadDetails

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class UploadInfo(id : BSONObjectID, uploadId : UploadId, reference : Reference, status : UploadStatus)

object UploadInfo {
  val status = "status"


  val format: Format[UploadInfo] = mongoEntity ( Json.format[UploadInfo] )
}


class FileUploadStatusRepository @Inject()(mongoComponent: ReactiveMongoComponent)(implicit ec : ExecutionContext)
  extends ReactiveRepository[UploadInfo, BSONObjectID](
    collectionName = "gatekeeperfileuploads",
    mongo = mongoComponent.mongoConnector.db,
    domainFormat = format,
    idFormat = ReactiveMongoFormats.objectIdFormats
  ) {

  def findByUploadId(uploadId: UploadId): Future[Option[UploadInfo]] =
    find("uploadId" -> Json.toJson(uploadId)).map(_.headOption)

  def updateStatus(reference : Reference, newStatus : UploadStatus): Future[UploadInfo] =
    for (result <- findAndUpdate(
      query = JsObject(Seq("reference" -> Json.toJson(reference))),
      update = Json.obj(
        "$set" -> Json.obj(
          status -> Json.toJson(newStatus)
        )
      ), upsert = true)) yield {
      result.result[UploadInfo].getOrElse(throw new Exception("Update failed, no document modified"))
    }

  def requestUpload(uploadInfo : UploadInfo): Future[Unit] =
    insert(uploadInfo).map(_ => ())


}