/*
 * Copyright 2021 HM Revenue & Customs
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
import org.mongodb.scala.ReadPreference.primaryPreferred
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.gatekeeperemail.models.Email
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport
import org.joda.time.DateTimeZone.UTC
import org.mongodb.scala.bson.BsonBoolean

class EmailRepositoryISpec extends AnyWordSpec with PlayMongoRepositorySupport[Email] with
  Matchers with BeforeAndAfterEach with GuiceOneAppPerSuite {
  val serviceRepo = repository.asInstanceOf[EmailRepository]

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

  override protected def repository: PlayMongoRepository[Email] = app.injector.instanceOf[EmailRepository]

  "persist" should {

    val email = Email("DL Team", List("test@digital.hmrc.gov.uk"), None, "markdownEmailBody", Some("This is test email"),
      "test subject", "composedBy", Some("approvedBy"), DateTime.now(UTC))

    "insert an Email message when it does not exist" in {
      await(serviceRepo.persist(email))

      val fetchedRecords = await(serviceRepo.collection.withReadPreference(primaryPreferred).find().toFuture())

      fetchedRecords.size shouldBe 1
      fetchedRecords.head shouldEqual email
    }

    "create index on composedBy" in {
      await(serviceRepo.persist(email))

      val Some(globalIdIndex) = await(serviceRepo.collection.listIndexes().toFuture()).find(i => i.get("name").get.asString().getValue == "composedByIndex")
      globalIdIndex.get("unique") shouldBe None
      globalIdIndex.get("background").get shouldBe BsonBoolean(true)
    }

    "create index on createDateTime" in {
      await(serviceRepo.persist(email))

      val Some(globalIdIndex) = await(serviceRepo.collection.listIndexes().toFuture()).find(i => i.get("name").get.asString().getValue == "createDateTimeIndex")
      globalIdIndex.get("unique") shouldBe None
      globalIdIndex.get("background").get shouldBe BsonBoolean(true)
    }
  }
}
