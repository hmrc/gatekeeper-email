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

import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromRegistries}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Updates.{combine, set}
import org.mongodb.scala.model.{FindOneAndUpdateOptions, IndexModel, IndexOptions, ReturnDocument}
import org.mongodb.scala.result.InsertOneResult
import org.mongodb.scala.{MongoClient, MongoCollection}
import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.models.{DraftEmail, EmailStatus, RegisteredUser}
import uk.gov.hmrc.gatekeeperemail.repositories.EmailMongoFormatter.emailFormatter
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, CollectionFactory, PlayMongoRepository}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DraftEmailRepository @Inject() (mongoComponent: MongoComponent, appConfig: AppConfig)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[DraftEmail](
      mongoComponent = mongoComponent,
      collectionName = "draftemails",
      domainFormat = emailFormatter,
      indexes = Seq(
        IndexModel(
          ascending("emailUUID"),
          IndexOptions()
            .name("emailUUIDIndex")
            .background(true)
            .unique(true)
        ),
        IndexModel(
          ascending("createDateTime"),
          IndexOptions()
            .name("ttlIndex")
            .expireAfter(appConfig.emailRecordRetentionPeriod * 365, TimeUnit.DAYS)
            .background(true)
        )
      )
    ) {

  override lazy val collection: MongoCollection[DraftEmail] =
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

  def persist(entity: DraftEmail): Future[InsertOneResult] = {
    collection.insertOne(entity).toFuture()
  }

  def getEmailData(emailUUID: String): Future[DraftEmail] = {
    def findByEmailUUID(emailUUID: UUID): Future[Option[DraftEmail]] = {
      collection.find(equal("emailUUID", Codecs.toBson(emailUUID))).headOption()
    }
    for (emailData <- findByEmailUUID(UUID.fromString(emailUUID))) yield {
      emailData match {
        case Some(email) => email
        case None        => throw new Exception(s"Email with id ${emailUUID} not found")
      }
    }
  }

  def updateEmailSentStatus(emailUUID: String, emailCount: Int): Future[DraftEmail] = {
    collection.findOneAndUpdate(
      equal("emailUUID", Codecs.toBson(emailUUID)),
      update = combine(
        set("status", Codecs.toBson(EmailStatus.SENT)),
        set("emailsCount", emailCount)
      ),
      options = FindOneAndUpdateOptions().upsert(false).returnDocument(ReturnDocument.AFTER)
    ).map(_.asInstanceOf[DraftEmail]).head()
  }

  def updateEmail(email: DraftEmail): Future[DraftEmail] = {
    collection.findOneAndUpdate(
      equal("emailUUID", Codecs.toBson(email.emailUUID)),
      update = combine(
        set("templateData", email.templateData),
        set("htmlEmailBody", email.htmlEmailBody),
        set("markdownEmailBody", email.markdownEmailBody),
        set("subject", email.subject),
        set("attachmentDetails", email.attachmentDetails.getOrElse(Seq.empty))
      ),
      options = FindOneAndUpdateOptions().upsert(false).returnDocument(ReturnDocument.AFTER)
    ).map(_.asInstanceOf[DraftEmail]).head()
  }

  def deleteByEmailUUID(emailUUID: String): Future[Boolean] = {
    collection.deleteOne(equal("emailUUID", Codecs.toBson(emailUUID))).head().map(x => x.getDeletedCount() == 1)
  }
}
