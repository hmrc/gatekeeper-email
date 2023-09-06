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

package uk.gov.hmrc.gatekeeperemail.repositories

import java.time.Clock
import java.time.Instant.now
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import com.mongodb.ReadPreference.primaryPreferred
import com.mongodb.client.model.ReturnDocument
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromRegistries}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Updates.{combine, set}
import org.mongodb.scala.model.{IndexModel, IndexOptions, _}
import org.mongodb.scala.result._
import org.mongodb.scala.{MongoClient, MongoCollection}

import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, CollectionFactory, PlayMongoRepository}

import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.models.{EmailStatus, SentEmail}

@Singleton
class SentEmailRepository @Inject() (mongoComponent: MongoComponent, appConfig: AppConfig, val clock: Clock)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[SentEmail](
      mongoComponent = mongoComponent,
      collectionName = "sentemails",
      domainFormat = SentEmail.format,
      replaceIndexes = true,
      indexes = Seq(
        IndexModel(
          ascending("status", "createdAt"),
          IndexOptions()
            .name("emailNextSendIndex")
            .background(true)
            .unique(false)
        ),
        IndexModel(
          ascending("createdAt"),
          IndexOptions()
            .name("ttlIndex")
            .expireAfter(appConfig.emailRecordRetentionPeriod * 365, TimeUnit.DAYS)
            .background(true)
            .unique(false)
        )
      )
    ) with MongoJavatimeFormats.Implicits {

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

  def findNextEmailToSend: Future[Option[SentEmail]] = {
    collection.withReadPreference(primaryPreferred)
      .find(filter = equal("status", Codecs.toBson(EmailStatus.PENDING)))
      .sort(ascending("createdAt"))
      .limit(1)
      .toFuture()
      .map(_.headOption)
  }

  def incrementFailedCount(email: SentEmail): Future[SentEmail] = {
    collection.withReadPreference(primaryPreferred)
      .findOneAndUpdate(
        filter = equal("id", Codecs.toBson(email.id)),
        update = combine(
          set("failedCount", email.failedCount + 1),
          set("updatedAt", Codecs.toBson(now(clock)))
        ),
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      )
      .head()
  }

  def persist(entity: List[SentEmail]): Future[InsertManyResult] = {
    collection.insertMany(entity).toFuture()
  }

  def markFailed(email: SentEmail): Future[SentEmail] = {
    collection.withReadPreference(primaryPreferred)
      .findOneAndUpdate(
        filter = equal("id", Codecs.toBson(email.id)),
        update = combine(
          set("status", Codecs.toBson(EmailStatus.FAILED)),
          set("updatedAt", Codecs.toBson(now(clock)))
        ),
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      )
      .head()
  }

  def markSent(email: SentEmail): Future[SentEmail] = {
    collection.withReadPreference(primaryPreferred)
      .findOneAndUpdate(
        filter = equal("id", Codecs.toBson(email.id)),
        update = combine(
          set("status", Codecs.toBson(EmailStatus.SENT)),
          set("updatedAt", Codecs.toBson(now(clock)))
        ),
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      )
      .head()
  }

  def fetchBatchOfNastyOldSentEmails(): Future[Seq[SentEmail]] = {
    collection
      .withReadPreference(primaryPreferred)
      .find(
        filter = exists("isUsingInstant", false)
      )
      .limit(50)
      .toFuture()
  }

  def persistBatchOfShinyConvertedSentEmails(sentEmails: Seq[SentEmail]): Future[Seq[SentEmail]] = {
    val results = sentEmails.map(mail =>
      collection
        .findOneAndReplace(
          filter = equal("id", Codecs.toBson(mail.id)),
          replacement = mail,
          options = FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER)
        ).head()
    )

    Future.sequence(results)
  }
}
