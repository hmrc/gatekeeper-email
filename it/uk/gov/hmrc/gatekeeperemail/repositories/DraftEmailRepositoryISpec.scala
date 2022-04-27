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

import java.time.{LocalDateTime, ZoneId}
import java.util.TimeZone

import akka.stream.Materializer
import org.mongodb.scala.ReadPreference.primaryPreferred
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.gatekeeperemail.models.{DraftEmail, EmailTemplateData, User}
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport
import org.mongodb.scala.bson.BsonBoolean

class DraftEmailRepositoryISpec extends AnyWordSpec with PlayMongoRepositorySupport[DraftEmail] with
  Matchers with BeforeAndAfterEach with GuiceOneAppPerSuite {
  val serviceRepo = repository.asInstanceOf[DraftEmailRepository]

  override implicit lazy val app: Application = appBuilder.build()
  implicit val materialiser: Materializer = app.injector.instanceOf[Materializer]

  override def beforeEach(): Unit = {
    prepareDatabase()
  }

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri" -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}"
      )

  override protected def repository: PlayMongoRepository[DraftEmail] = app.injector.instanceOf[DraftEmailRepository]

  "persist" should {
    val templateData = EmailTemplateData("templateId", Map(), false, Map(), None)
    val users = List(User("example@example.com", "first name", "last name", true),
      User("example2@example2.com", "first name2", "last name2", true))
    val email = DraftEmail("emailId-123", templateData, "DL Team", users, None, "markdownEmailBody", "This is test email",
      "test subject", "test status", "composedBy", Some("approvedBy"), LocalDateTime.now())

    "insert an Email message when it does not exist" in {
      await(serviceRepo.persist(email))

      val fetchedRecords = await(serviceRepo.collection.withReadPreference(primaryPreferred).find().toFuture())

      fetchedRecords.size shouldBe 1
      fetchedRecords.head shouldEqual email
    }

    "create index on emailUUID" in {
      await(serviceRepo.persist(email))

      val Some(globalIdIndex) = await(serviceRepo.collection.listIndexes().toFuture()).find(i => i.get("name").get.asString().getValue == "emailUUIDIndex")
      globalIdIndex.get("unique") shouldBe Some(BsonBoolean(value=true))
      globalIdIndex.get("background").get shouldBe BsonBoolean(true)
    }
  }
}
