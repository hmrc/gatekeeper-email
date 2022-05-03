<<<<<<< HEAD
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

import java.util.concurrent.TimeUnit

import com.mongodb.ReadPreference.primaryPreferred
import com.mongodb.client.model.ReturnDocument
import javax.inject.{Inject, Singleton}
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromRegistries}
import org.mongodb.scala.model._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Updates.{combine, set}
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.mongodb.scala.result.InsertOneResult
import org.mongodb.scala.{MongoClient, MongoCollection}
import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.models.{EmailStatus, SentEmail}
import uk.gov.hmrc.gatekeeperemail.repositories.ReadyEmailFormatter.readyEmailFormatter
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, CollectionFactory, PlayMongoRepository}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SentEmailRepository @Inject()(mongoComponent: MongoComponent, appConfig: AppConfig)
                                   (implicit ec: ExecutionContext)
  extends PlayMongoRepository[SentEmail](
    mongoComponent = mongoComponent,
    collectionName = "sentemails",
    domainFormat = readyEmailFormatter,
    indexes = Seq(IndexModel(ascending("status",  "createdAt"),
        IndexOptions()
          .name("emailNextSendIndex")
          .background(true)
          .unique(false)),
      IndexModel(ascending("ttlIndex"),
        IndexOptions()
          .name("ttlIndex")
          .expireAfter(appConfig.emailRecordRetentionPeriod * 365, TimeUnit.DAYS)
          .background(true)
          .unique(false)))) {

  override lazy val collection: MongoCollection[SentEmail] =
    CollectionFactory
      .collection(mongoComponent.database, collectionName, domainFormat)
      .withCodecRegistry(
        fromRegistries(
          fromCodecs(
            Codecs.playFormatCodec(domainFormat),
            Codecs.playFormatCodec(EmailStatus.jsonFormat)
          ),
          MongoClient.DEFAULT_CODEC_REGISTRY
        )
      )

  def persist(entity: SentEmail): Future[InsertOneResult] = {
    collection.insertOne(entity).toFuture()
  }

  def findNextEmailToSend: Future[Option[SentEmail]] = {
    collection.withReadPreference(primaryPreferred)
    .find(filter = equal("status", Codecs.toBson(EmailStatus.IN_PROGRESS)))
      .sort(ascending("createdAt"))
      .limit(1)
      .toFuture()
      .map(_.headOption)
  }

  def incrementFailedCount(email: SentEmail): Future[SentEmail] = {
    collection.withReadPreference(primaryPreferred)
      .findOneAndUpdate(filter = equal("id", Codecs.toBson(email.id)),
        update = set("failedCount", email.failedCount + 1) ,
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER))
      .head()
  }

  def markFailed(email: SentEmail): Future[SentEmail] = {
    collection.withReadPreference(primaryPreferred)
      .findOneAndUpdate(filter = equal("id", Codecs.toBson(email.id)),
        update = combine(set("failedCount", email.failedCount + 1), set("status", Codecs.toBson(EmailStatus.FAILED))),
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER))
      .head()
  }

  def markSent(email: SentEmail): Future[SentEmail] = {
    collection.withReadPreference(primaryPreferred)
      .findOneAndUpdate(filter = equal("id", Codecs.toBson(email.id)),
        update = set("status", Codecs.toBson(EmailStatus.SENT)),
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER))
      .head()
  }
}
