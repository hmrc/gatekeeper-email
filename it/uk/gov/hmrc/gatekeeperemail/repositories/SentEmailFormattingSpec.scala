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

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID

import org.mongodb.scala.{Document, bson}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.mongo.play.json.Codecs
import uk.gov.hmrc.mongo.test.MongoSupport

import uk.gov.hmrc.gatekeeperemail.common.AsyncHmrcSpec
import uk.gov.hmrc.gatekeeperemail.models.requests.DevelopersEmailQuery
import uk.gov.hmrc.gatekeeperemail.models.{EmailStatus, EmailTemplateData, SentEmail}
import uk.gov.hmrc.gatekeeperemail.scheduled.SchedulerModule

class SentEmailFormattingSpec extends AsyncHmrcSpec with GuiceOneServerPerSuite with BeforeAndAfterEach with MongoSupport {
  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder = {
    GuiceApplicationBuilder()
      .configure(
        "metrics.jvm" -> false,
        "mongodb.uri" -> s"mongodb://localhost:27017/test-${this.getClass.getSimpleName}"
      )
      .disable(classOf[SchedulerModule])
  }

  val sentEmailRepository = app.injector.instanceOf[SentEmailRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sentEmailRepository.collection.drop().toFuture())
    await(sentEmailRepository.ensureIndexes)
  }

  trait Setup {
    val createdAt = LocalDateTime.now()
    val updatedAt = LocalDateTime.now()

    val emailUuid        = UUID.randomUUID
    val templateData     = EmailTemplateData("templateId", Map(), false, Map(), None)
    val emailPreferences = DevelopersEmailQuery()

    val email = SentEmail(
      updatedAt = updatedAt.toInstant(ZoneOffset.UTC),
      emailUuid = emailUuid,
      firstName = "first",
      lastName = "last",
      recipient = "first.last@digital.hmrc.gov.uk",
      status = EmailStatus.PENDING,
      failedCount = 0,
      id = UUID.randomUUID(),
      createdAt = createdAt.toInstant(ZoneOffset.UTC),
      isUsingInstant = None
    )
  }

  "Sent email repository" should {
    "read records correctly when the createdAt and updatedAt are Strings" in new Setup {
      await(mongoDatabase.getCollection("sentemails").insertOne(Document(
        RawEmailTestData.toJsonObject(email, createdAt, updatedAt, isUsingInstant = false).toString()
      )).toFuture())

      val readEmail = await(sentEmailRepository.collection.find(bson.Document("emailUuid" -> Codecs.toBson(email.emailUuid))).head())

      readEmail shouldBe email.copy(isUsingInstant = Some(false))
    }

    "read records correctly when the createdAt and updatedAt are ISODateTimes" in new Setup {
      await(mongoDatabase.getCollection("sentemails").insertOne(Document(
        RawEmailTestData.toJsonObject(email, createdAt, updatedAt, isUsingInstant = true).toString()
      )).toFuture())

      val readEmail = await(sentEmailRepository.collection.find(bson.Document("emailUuid" -> Codecs.toBson(email.emailUuid))).head())

      readEmail shouldBe email.copy(isUsingInstant = Some(true))
    }

    "write records with createdAt and updatedAt as ISODateTimes" in new Setup {
      Json.prettyPrint(Json.toJson(email)) shouldBe RawEmailTestData.toJsonString(email)
    }
  }
}
