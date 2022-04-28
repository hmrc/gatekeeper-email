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

import java.time.LocalDateTime
import java.util.UUID

import org.mongodb.scala.ReadPreference.primaryPreferred
import org.mongodb.scala.bson.{BsonBoolean, BsonInt64}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.gatekeeperemail.models.EmailStatus._
import uk.gov.hmrc.gatekeeperemail.models.SentEmail
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport

class SentEmailRepositoryISpec extends AnyWordSpec with PlayMongoRepositorySupport[SentEmail] with
  Matchers with BeforeAndAfterEach with GuiceOneAppPerSuite {
  val serviceRepo = repository.asInstanceOf[SentEmailRepository]

  override implicit lazy val app: Application = appBuilder.build()

  override def beforeEach(): Unit = {
    prepareDatabase()
  }

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri" -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}"
      )

  override protected def repository: PlayMongoRepository[SentEmail] = app.injector.instanceOf[SentEmailRepository]

  val sentEmail = SentEmail(createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now(), emailUuid = UUID.randomUUID(),
    firstName = "first", lastName = "last", recipient = "first.last@digital.hmrc.gov.uk", status = IN_PROGRESS,
    failedCount = 0)

  "persist" should {
    "insert a sent email message when it does not exist" in {
      await(serviceRepo.persist(sentEmail))

      val fetchedRecords = await(serviceRepo.collection.withReadPreference(primaryPreferred).find().toFuture())

      fetchedRecords.size shouldBe 1
      fetchedRecords.head shouldBe sentEmail
    }

    "create index on status plus createdAt" in {
      await(serviceRepo.persist(sentEmail))

      val Some(emailToSendNextIndex) = await(serviceRepo.collection.listIndexes().toFuture())
        .find(i => i.get("name").get.asString().getValue == "emailNextSendIndex")
      emailToSendNextIndex.get("unique") shouldBe None
      emailToSendNextIndex.get("background").get shouldBe BsonBoolean(true)
    }

    "create a TTL index on createdAt" in {
      await(serviceRepo.persist(sentEmail))

      val Some(ttlIndex) = await(serviceRepo.collection.listIndexes().toFuture())
        .find(i => i.get("name").get.asString().getValue == "ttlIndex")
      ttlIndex.get("unique") shouldBe None
      ttlIndex.get("background").get shouldBe BsonBoolean(true)
      ttlIndex.get("expireAfterSeconds") shouldBe Some(BsonInt64(60 * 60 * 24 * 365 * 7))
    }
  }

  "findNextEmailToSend" should {
    "find the oldest pending email" in {
      val expectedNextSendRecipient = "old.email@digital.hmrc.gov.uk"
      await(serviceRepo.persist(sentEmail.copy(createdAt = LocalDateTime.now().minusMinutes(10), recipient = expectedNextSendRecipient)))
      await(serviceRepo.persist(sentEmail))

      val Some(nextEmail) = await(serviceRepo.findNextEmailToSend)

      nextEmail.recipient shouldBe expectedNextSendRecipient
      nextEmail.status shouldBe IN_PROGRESS
    }

    "ignore emails with failed status" in {
      await(serviceRepo.persist(sentEmail.copy(status = FAILED, recipient = "failed.send@abc.com")))
      await(serviceRepo.persist(sentEmail))

      val Some(nextEmail) = await(serviceRepo.findNextEmailToSend)

      nextEmail.recipient shouldBe sentEmail.recipient
      nextEmail.status shouldBe IN_PROGRESS
    }

    "handle documents with the same created time" in {
      await(serviceRepo.persist(sentEmail.copy(recipient = "failed.send@abc.com")))
      await(serviceRepo.persist(sentEmail))

      val Some(nextEmail) = await(serviceRepo.findNextEmailToSend)

      nextEmail.status shouldBe IN_PROGRESS
    }
  }

  "updateFailedCount" should {
    "increment the fails counter" in {
      await(serviceRepo.persist(sentEmail))

      val Some(nextEmail) = await(serviceRepo.findNextEmailToSend)

      await(serviceRepo.updateFailedCount(nextEmail))

      val fetchedRecords = await(serviceRepo.collection.withReadPreference(primaryPreferred).find().toFuture())
      fetchedRecords.size shouldBe 1
      fetchedRecords.head.failedCount shouldBe 1
    }
  }

  "markFailed" should {
    "increment the fails counter and mark the document as FAILED" in {
      await(serviceRepo.persist(sentEmail))

      val Some(nextEmail) = await(serviceRepo.findNextEmailToSend)

      await(serviceRepo.markFailed(nextEmail))

      val fetchedRecords = await(serviceRepo.collection.withReadPreference(primaryPreferred).find().toFuture())
      fetchedRecords.size shouldBe 1
      fetchedRecords.head.failedCount shouldBe 1
      fetchedRecords.head.status shouldBe FAILED
    }
  }

  "markSent" should {
    "leave unchanged the fails counter and mark the document as SENT" in {
      await(serviceRepo.persist(sentEmail))

      val Some(nextEmail) = await(serviceRepo.findNextEmailToSend)

      await(serviceRepo.markSent(nextEmail))

      val fetchedRecords = await(serviceRepo.collection.withReadPreference(primaryPreferred).find().toFuture())
      fetchedRecords.size shouldBe 1
      fetchedRecords.head.failedCount shouldBe 0
      fetchedRecords.head.status shouldBe SENT
    }
  }
}
