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

import org.mongodb.scala.ReadPreference.primaryPreferred
import org.mongodb.scala.bson.{BsonBoolean, BsonDocument}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport

import uk.gov.hmrc.gatekeeperemail.models.EmailStatus._
import uk.gov.hmrc.gatekeeperemail.models.SentEmail

class SentEmailRepositoryISpec
    extends AnyWordSpec
    with PlayMongoRepositorySupport[SentEmail]
    with Matchers
    with BeforeAndAfterEach
    with GuiceOneAppPerSuite
    with FixedClock
    with OptionValues {

  lazy val serviceRepo = repository.asInstanceOf[SentEmailRepository]

  override implicit lazy val app: Application = appBuilder.build()

  override def beforeEach(): Unit = {
    prepareDatabase()
  }

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri" -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}"
      )

  override protected val repository: PlayMongoRepository[SentEmail] = app.injector.instanceOf[SentEmailRepository]

  val sentEmail = List(SentEmail(
    createdAt = precise(),
    updatedAt = precise(),
    emailUuid = UUID.randomUUID(),
    firstName = "first",
    lastName = "last",
    recipient = "first.last@digital.hmrc.gov.uk",
    status = PENDING,
    failedCount = 0
  ))

  "persist" should {
    "insert a sent email message when it does not exist" in {
      val inserted = await(serviceRepo.persist(sentEmail))

      val fetchedRecords = await(serviceRepo.collection.withReadPreference(primaryPreferred()).find().toFuture())

      inserted.wasAcknowledged() shouldBe true
      fetchedRecords.size shouldBe 1
      fetchedRecords.head shouldBe sentEmail.head
    }

    "create index on status plus createdAt" in {
      await(serviceRepo.persist(sentEmail))

      val emailToSendNextIndex = await(serviceRepo.collection.listIndexes().toFuture())
        .find(i => i.get("name").get.asString().getValue == "emailNextSendIndex").get
      emailToSendNextIndex.get("unique") shouldBe None
      emailToSendNextIndex.get("background").get shouldBe BsonBoolean(true)
    }

    "create a TTL index on createdAt" in {
      await(serviceRepo.persist(sentEmail))

      val ttlIndex = await(serviceRepo.collection.listIndexes().toFuture())
        .find(i => i.get("name").get.asString().getValue == "ttlIndex").get

      ttlIndex.get("key").get shouldBe BsonDocument("createdAt" -> Codecs.toBson(1))
      ttlIndex.get("unique") shouldBe None
      ttlIndex.get("background").get shouldBe BsonBoolean(true)
      ttlIndex.get("expireAfterSeconds").value.asNumber().intValue() shouldBe 60 * 60 * 24 * 365 * 7
    }
  }

  "findNextEmailToSend" should {
    "find the oldest pending email" in {
      val expectedNextSendRecipient = "old.email@digital.hmrc.gov.uk"
      val emailsToSend              = List(sentEmail.head.copy(createdAt = precise().minusSeconds(10 * 60), recipient = expectedNextSendRecipient))
      await(serviceRepo.persist(emailsToSend))
      await(serviceRepo.persist(sentEmail))

      val nextEmail = await(serviceRepo.findNextEmailToSend)

      nextEmail.value.recipient shouldBe expectedNextSendRecipient
      nextEmail.value.status shouldBe PENDING
    }

    "ignore emails with failed status" in {
      val emailsToSend = List(sentEmail.head.copy(status = FAILED, recipient = "failed.send@abc.com"))
      await(serviceRepo.persist(emailsToSend))
      await(serviceRepo.persist(sentEmail))

      val nextEmail = await(serviceRepo.findNextEmailToSend)

      nextEmail.value.recipient shouldBe sentEmail.head.recipient
      nextEmail.value.status shouldBe PENDING
    }

    "handle documents with the same created time" in {
      val emailsToSend = List(sentEmail.head.copy(recipient = "failed.send@abc.com"))
      await(serviceRepo.persist(emailsToSend))
      await(serviceRepo.persist(sentEmail))

      val nextEmail = await(serviceRepo.findNextEmailToSend)

      nextEmail.value.status shouldBe PENDING
    }
  }

  "updateFailedCount" should {
    "increment the fails counter" in {
      await(serviceRepo.persist(sentEmail))

      val nextEmail = await(serviceRepo.findNextEmailToSend)

      nextEmail shouldBe defined

      await(serviceRepo.incrementFailedCount(nextEmail.value))

      val fetchedRecords = await(serviceRepo.collection.withReadPreference(primaryPreferred()).find().toFuture())
      fetchedRecords.size shouldBe 1
      fetchedRecords.head.failedCount shouldBe 1
    }
  }

  "markFailed" should {
    "increment the fails counter and mark the document as FAILED" in {
      await(serviceRepo.persist(sentEmail))

      val nextEmail = await(serviceRepo.findNextEmailToSend)

      nextEmail shouldBe defined

      await(serviceRepo.markFailed(nextEmail.value))

      val fetchedRecords = await(serviceRepo.collection.withReadPreference(primaryPreferred()).find().toFuture())
      fetchedRecords.size shouldBe 1
      fetchedRecords.head.failedCount shouldBe 0
      fetchedRecords.head.status shouldBe FAILED
    }
  }

  "markSent" should {
    "leave unchanged the fails counter and mark the document as SENT" in {
      await(serviceRepo.persist(sentEmail))

      val nextEmail = await(serviceRepo.findNextEmailToSend)

      nextEmail shouldBe defined

      await(serviceRepo.markSent(nextEmail.value))

      val fetchedRecords = await(serviceRepo.collection.withReadPreference(primaryPreferred()).find().toFuture())
      fetchedRecords.size shouldBe 1
      fetchedRecords.head.failedCount shouldBe 0
      fetchedRecords.head.status shouldBe SENT
    }
  }
}
