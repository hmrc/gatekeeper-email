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

import javax.inject.{Inject, Singleton}
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromRegistries}
import org.joda.time.{DateTime, Days}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.{FindOneAndUpdateOptions, IndexModel, IndexOptions, InsertManyOptions, ReturnDocument}
import org.mongodb.scala.result.{InsertManyResult, InsertOneResult}
import org.mongodb.scala.{MongoClient, MongoCollection, SingleObservable}
import play.api.{Logger, Logging}
import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.models.{Email, EmailStatus, SentEmail}
import uk.gov.hmrc.gatekeeperemail.repositories.SentEmailFormatter.sentEmailFormatter
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, CollectionFactory, PlayMongoRepository}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SentEmailRepository @Inject()(mongoComponent: MongoComponent, appConfig: AppConfig)
                                   (implicit ec: ExecutionContext)
  extends PlayMongoRepository[SentEmail](
    mongoComponent = mongoComponent,
    collectionName = "sentemails",
    domainFormat = sentEmailFormatter,
    indexes = Seq(IndexModel(ascending("status", "failedCount", "createdAt"),
      IndexOptions()
        .name("emailNextSendIndex")
        .background(true))
    )) {
  val logger: Logger = Logger(getClass.getName)

  override lazy val collection: MongoCollection[SentEmail] =
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
            Codecs.playFormatCodec(EmailMongoFormatter.emailFormatter)
          ),
          MongoClient.DEFAULT_CODEC_REGISTRY
        )
      )

  def findNextEmail(email: SentEmail): Future[SentEmail] = {
    collection.withReadPreference(primaryPreferred)
      .find(filter = and(equal("status", EmailStatus.INPROGRESS),
        and(lt("failedCount", 3))))
      .sort(ascending("createdAt"))
      .map(_.asInstanceOf[SentEmail]).head()
  }

  def persist(entity: List[SentEmail]): Future[InsertManyResult] = {
    collection.insertMany(entity).toFuture()
  }

}
