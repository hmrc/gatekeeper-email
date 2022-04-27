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

import akka.stream.Materializer
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.mongodb.scala.ReadPreference.primaryPreferred
import org.mongodb.scala.bson.BsonBoolean
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.gatekeeperemail.models.{Email, EmailTemplateData, SentEmail, User}
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport

import java.time.LocalDateTime
import java.util.UUID

class SentEmailRepositoryISpec extends AnyWordSpec with PlayMongoRepositorySupport[SentEmail] with
  Matchers with BeforeAndAfterEach with GuiceOneAppPerSuite {
  val serviceRepo = repository.asInstanceOf[SentEmailRepository]

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

  override protected def repository: PlayMongoRepository[SentEmail] = app.injector.instanceOf[SentEmailRepository]

  "persist" should {
    val templateData = EmailTemplateData("templateId", Map(), false, Map(), None)
    val users = List(User("example@example.com", "first name", "last name", true),
      User("example2@example2.com", "first name2", "last name2", true))
    val email = Email("f01709c9-3ecd-4236-bd15-1f200bb374f1", templateData, "DL Team", users, None, "markdownEmailBody", "This is test email",
      "test subject", "test status", "composedBy", Some("approvedBy"), DateTime.now(UTC))
    val sentEmail = SentEmail(LocalDateTime.now(), LocalDateTime.now(), UUID.fromString(email.emailUUID),
      email.recipients.head.firstName, email.recipients.head.lastName, email.recipients.head.email, "PENDING", 0)

    "insert an Sent Email message when it does not exist" in {
      await(serviceRepo.persist(sentEmail))

      val fetchedRecords = await(serviceRepo.collection.withReadPreference(primaryPreferred).find().toFuture())

      fetchedRecords.size shouldBe 1
      fetchedRecords.head shouldEqual email
    }

    "create index on emailUUID" in {
      await(serviceRepo.persist(sentEmail))

      val Some(globalIdIndex) = await(serviceRepo.collection.listIndexes().toFuture()).find(i => i.get("name").get.asString().getValue == "emailNextSendIndex")
      globalIdIndex.get("background").get shouldBe BsonBoolean(true)
    }
  }
}
