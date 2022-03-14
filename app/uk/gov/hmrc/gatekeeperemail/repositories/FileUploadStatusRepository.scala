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

package uk.gov.hmrc.gatekeeperemail.repositories

import akka.stream.Materializer
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromRegistries}
import org.joda.time.DateTime
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.{FindOneAndUpdateOptions, IndexModel, IndexOptions, ReturnDocument}
import org.mongodb.scala.{MongoClient, MongoCollection}
import play.api.libs.json.Format
import uk.gov.hmrc.gatekeeperemail.models.{Reference, UploadId, UploadStatus}
import uk.gov.hmrc.gatekeeperemail.repositories.FileUploadMongoFormatter._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, CollectionFactory, PlayMongoRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.Json
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats

case class UploadInfo(reference : String, emailUUID: String, status : UploadStatus)

object UploadInfo {
  val status = "status"
  implicit val dateFormation : Format[DateTime] = MongoJodaFormats.dateTimeFormat
  implicit val format: Format[UploadInfo] =  Json.format[UploadInfo]
}

@Singleton
class FileUploadStatusRepository @Inject()(mongoComponent: MongoComponent)
                                          (implicit ec : ExecutionContext, m: Materializer)
  extends PlayMongoRepository[UploadInfo](
    mongoComponent = mongoComponent,
    collectionName = "fileuploads",
    domainFormat = uploadInfo,
    indexes = Seq(IndexModel(ascending("reference"),
      IndexOptions().name("referenceIndex").background(true).unique(true)),
      IndexModel(ascending("emailUUID"),
        IndexOptions().name("emailUUIDIndex").background(true).unique(false)))
  ) {

  override lazy val collection: MongoCollection[UploadInfo] =
    CollectionFactory
      .collection(mongoComponent.database, collectionName, domainFormat)
      .withCodecRegistry(
        fromRegistries(
          fromCodecs(
            Codecs.playFormatCodec(referenceFormat),
            Codecs.playFormatCodec(bsonFormat),
            Codecs.playFormatCodec(domainFormat),
            Codecs.playFormatCodec(uploadStatusFormat),
            Codecs.playFormatCodec(initiateFormat),
            Codecs.playFormatCodec(failedFormat),
            Codecs.playFormatCodec(uploadedSuccessfullyFormat),
              Codecs.playFormatCodec(uploadedFailedFormat),
              Codecs.playFormatCodec(dateFormation)
          ),
          MongoClient.DEFAULT_CODEC_REGISTRY
        )
      )

  def findByUploadId(reference: String): Future[Option[UploadInfo]] =
    collection.find(equal("reference" , Codecs.toBson(reference))).headOption()

  def updateStatus(reference : String, newStatus : UploadStatus): Future[UploadInfo] = {
    collection.findOneAndUpdate(equal("reference", Codecs.toBson(reference)),
      update = set("status", newStatus),
      options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      ).map(_.asInstanceOf[UploadInfo]).head()
  }
  def requestUpload(uploadInfo : UploadInfo): Future[UploadInfo] =
    collection.insertOne(uploadInfo).toFuture().map(res => uploadInfo)
}
