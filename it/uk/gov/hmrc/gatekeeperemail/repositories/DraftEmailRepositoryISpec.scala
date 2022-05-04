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
import uk.gov.hmrc.gatekeeperemail.models.{DraftEmail, EmailStatus, EmailTemplateData, User}
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport

class DraftEmailRepositoryISpec extends AnyWordSpec with PlayMongoRepositorySupport[DraftEmail] with
  Matchers with BeforeAndAfterEach with GuiceOneAppPerSuite {
  val serviceRepo = repository.asInstanceOf[DraftEmailRepository]

  override implicit lazy val app: Application = appBuilder.build()

  override def beforeEach(): Unit = {
    prepareDatabase()
  }

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri" -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}"
      )

  override protected def repository: PlayMongoRepository[DraftEmail] = app.injector.instanceOf[DraftEmailRepository]

  trait Setup {
    val templateData = EmailTemplateData("templateId", Map(), false, Map(), None)
    val users = List(User("example@example.com", "first name", "last name", true),
      User("example2@example2.com", "first name2", "last name2", true))
    val email = DraftEmail(UUID.randomUUID.toString(), templateData, "DL Team", users, None, "markdownEmailBody", "This is test email",
      "test subject", EmailStatus.FAILED, "composedBy", Some("approvedBy"), LocalDateTime.now())

  }
  "persist" should {

    "insert an Email message when it does not exist" in new Setup{
      await(serviceRepo.persist(email))

      val fetchedRecords = await(serviceRepo.collection.withReadPreference(primaryPreferred).find().toFuture())

      fetchedRecords.size shouldBe 1
      fetchedRecords.head shouldEqual email
    }

    "create index on emailUUID" in new Setup {
      await(serviceRepo.persist(email))

      val Some(globalIdIndex) = await(serviceRepo.collection.listIndexes().toFuture()).find(i => i.get("name").get.asString().getValue == "emailUUIDIndex")
      globalIdIndex.get("unique") shouldBe Some(BsonBoolean(value=true))
      globalIdIndex.get("background").get shouldBe BsonBoolean(true)
    }

    "create TTL index on createDateTime" in new Setup {
      await(serviceRepo.persist(email))

      val Some(globalIdIndex) = await(serviceRepo.collection.listIndexes().toFuture()).find(i => i.get("name").get.asString().getValue == "ttlIndex")
      globalIdIndex.get("unique") shouldBe None
      globalIdIndex.get("background").get shouldBe BsonBoolean(true)
    }
  }

  "getEmailData" should {
    "find email data when it exists" in new Setup{
      await(serviceRepo.persist(email))

      val emailData = await(serviceRepo.getEmailData(email.emailUUID))

      emailData shouldBe email
    }

    "throw exception when email data cannot be found" in new Setup{
      val exception:Exception = intercept[Exception] {
        await(serviceRepo.getEmailData(email.emailUUID))
      }
      exception.getMessage shouldBe s"Email with id ${email.emailUUID} not found"
    }
  }
}
