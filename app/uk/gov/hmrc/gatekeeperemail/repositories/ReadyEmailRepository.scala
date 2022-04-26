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

import com.mongodb.ReadPreference.primaryPreferred
import javax.inject.{Inject, Singleton}
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromRegistries}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.mongodb.scala.result.InsertOneResult
import org.mongodb.scala.{MongoClient, MongoCollection}
import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.models.{EmailStatus, ReadyEmail}
import uk.gov.hmrc.gatekeeperemail.repositories.ReadyEmailFormatter.readyEmailFormatter
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, CollectionFactory, PlayMongoRepository}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReadyEmailRepository @Inject()(mongoComponent: MongoComponent, appConfig: AppConfig)
                                    (implicit ec: ExecutionContext)
  extends PlayMongoRepository[ReadyEmail](
    mongoComponent = mongoComponent,
    collectionName = "readyemails",
    domainFormat = readyEmailFormatter,
    indexes = Seq(IndexModel(ascending("status",  "createdAt"),
        IndexOptions()
          .name("emailNextSendIndex")
          .background(true)
          .unique(false)),
      )) {

  override lazy val collection: MongoCollection[ReadyEmail] =
    CollectionFactory
      .collection(mongoComponent.database, collectionName, domainFormat)
      .withCodecRegistry(
        fromRegistries(
          fromCodecs(
            Codecs.playFormatCodec(domainFormat),
            Codecs.playFormatCodec(EmailMongoFormatter.emailTemplateDataFormatter),
            Codecs.playFormatCodec(EmailMongoFormatter.cargoFormat),
            Codecs.playFormatCodec(EmailMongoFormatter.attachmentDetailsFormat),
            Codecs.playFormatCodec(EmailMongoFormatter.attachmentDetailsWithObjectStoreFormat),
            Codecs.playFormatCodec(EmailMongoFormatter.emailFormatter),
            Codecs.playFormatCodec(EmailStatus.jsonFormat)
          ),
          MongoClient.DEFAULT_CODEC_REGISTRY
        )
      )

  def persist(entity: ReadyEmail): Future[InsertOneResult] = {
    collection.insertOne(entity).toFuture()
  }

  def findNextEmailToSend: Future[ReadyEmail] = {
    collection.withReadPreference(primaryPreferred)
    .find(filter = equal("status", Codecs.toBson(EmailStatus.IN_PROGRESS)))
      .sort(ascending("createdAt"))
      .limit(1)
      .head()
  }

}