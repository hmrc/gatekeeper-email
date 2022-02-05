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

import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromRegistries}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.{MongoClient, MongoCollection}
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.{FindOneAndUpdateOptions, IndexModel, IndexOptions, ReturnDocument}
import org.mongodb.scala.result.InsertOneResult
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Results.BadRequest
import uk.gov.hmrc.gatekeeperemail.models.{Email, EmailRequest, EmailSaved}
import uk.gov.hmrc.gatekeeperemail.repositories.EmailMongoFormatter.emailFormatter
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, CollectionFactory, PlayMongoRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailRepository @Inject()(mongoComponent: MongoComponent)
                                             (implicit ec: ExecutionContext)
  extends PlayMongoRepository[Email](
    mongoComponent = mongoComponent,
    collectionName = "email",
    domainFormat = emailFormatter,
    indexes = Seq(IndexModel(ascending("composedBy"),
      IndexOptions().name("composedByIndex").background(true)),
      IndexModel(ascending("createDateTime"),
        IndexOptions().name("createDateTimeIndex").background(true).unique(false)))
  ) {

  override lazy val collection: MongoCollection[Email] =
    CollectionFactory
      .collection(mongoComponent.database, collectionName, domainFormat)
      .withCodecRegistry(
        fromRegistries(
          fromCodecs(
            Codecs.playFormatCodec(domainFormat),
            Codecs.playFormatCodec(EmailMongoFormatter.emailFormatter)
          ),
          MongoClient.DEFAULT_CODEC_REGISTRY
        )
      )

    def persist(entity: Email): Future[InsertOneResult] = {
      collection.insertOne(entity).toFuture()
    }

    def getEmailData(savedEmail: EmailSaved): Future[Email] = {
      for (emailData <- findByEmailUID(savedEmail.emailUID)) yield {
        emailData match {
          case Some(email) => email
          case None         => throw new Exception(s"Email with id ${savedEmail.emailUID} not found")
        }
      }
    }

  def updateEmail(email: EmailRequest, emailSaved: EmailSaved, key: String): Future[Email] = {

    collection.findOneAndUpdate(equal("emailUID", Codecs.toBson(emailSaved.emailUID)),
      update = set("emailData", email.emailData),
      options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
    ).map(_.asInstanceOf[Email]).head()
  }

  def findByEmailUID(emailUID: String)(implicit executionContext: ExecutionContext): Future[Option[Email]] = {
    collection.find(equal("emailUID", Codecs.toBson(emailUID))).headOption()
  }
}