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
import org.mongodb.scala.bson.BsonBoolean
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.gatekeeperemail.models.EmailStatus._
import uk.gov.hmrc.gatekeeperemail.models.ReadyEmail
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport

class ReadyEmailRepositoryISpec extends AnyWordSpec with PlayMongoRepositorySupport[ReadyEmail] with
  Matchers with BeforeAndAfterEach with GuiceOneAppPerSuite {
  val serviceRepo = repository.asInstanceOf[ReadyEmailRepository]

  override implicit lazy val app: Application = appBuilder.build()

  override def beforeEach(): Unit = {
    prepareDatabase()
  }

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri" -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}"
      )

  override protected def repository: PlayMongoRepository[ReadyEmail] = app.injector.instanceOf[ReadyEmailRepository]

  val email = ReadyEmail(createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now(), emailUuid = UUID.randomUUID(),
    firstName = "first", lastName = "last", recipient = "first.last@digital.hmrc.gov.uk", status = IN_PROGRESS,
    failedCount = 0)

  "persist" should {
    "insert a ready email message when it does not exist" in {
      await(serviceRepo.persist(email))

      val fetchedRecords = await(serviceRepo.collection.withReadPreference(primaryPreferred).find().toFuture())

      fetchedRecords.size shouldBe 1
      fetchedRecords.head shouldEqual email
    }

    "create index on status plus createdAt" in {
      await(serviceRepo.persist(email))

      val Some(emailToSendNextIndex) = await(serviceRepo.collection.listIndexes().toFuture())
        .find(i => i.get("name").get.asString().getValue == "emailNextSendIndex")
      emailToSendNextIndex.get("unique") shouldBe None
      emailToSendNextIndex.get("background").get shouldBe BsonBoolean(true)
    }
  }

  "findNextEmailToSend" should {
    "find the oldest pending email" in {
      val expectedNextSendRecipient = "old.email@digital.hmrc.gov.uk"
      await(serviceRepo.persist(email.copy(createdAt = LocalDateTime.now().minusMinutes(10), recipient = expectedNextSendRecipient)))
      await(serviceRepo.persist(email))

      val nextEmail = await(serviceRepo.findNextEmailToSend)

      nextEmail.recipient shouldBe expectedNextSendRecipient
      nextEmail.status shouldBe IN_PROGRESS
    }

    "ignore emails with failed status" in {
      await(serviceRepo.persist(email.copy(status = FAILED, recipient = "failed.send@abc.com")))
      await(serviceRepo.persist(email))

      val nextEmail = await(serviceRepo.findNextEmailToSend)

      nextEmail.recipient shouldBe email.recipient
      nextEmail.status shouldBe IN_PROGRESS
    }

    "handle documents with the same created time" in {
      await(serviceRepo.persist(email.copy(recipient = "failed.send@abc.com")))
      await(serviceRepo.persist(email))

      val nextEmail = await(serviceRepo.findNextEmailToSend)

      nextEmail.status shouldBe IN_PROGRESS
    }
  }

  "updateFailedCount" should {
    "update the fails counter" in {
      await(serviceRepo.persist(email))

      val nextEmail = await(serviceRepo.findNextEmailToSend)

      await(serviceRepo.updateFailedCount(nextEmail))

      val fetchedRecords = await(serviceRepo.collection.withReadPreference(primaryPreferred).find().toFuture())
      fetchedRecords.size shouldBe 1
      fetchedRecords.head.failedCount shouldBe 1
    }
  }
}
