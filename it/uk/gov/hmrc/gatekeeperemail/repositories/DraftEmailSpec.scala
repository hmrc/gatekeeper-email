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

import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.gatekeeperemail.scheduled.SchedulerModule
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.gatekeeperemail.common.AsyncHmrcSpec
import uk.gov.hmrc.gatekeeperemail.models.DraftEmail
import uk.gov.hmrc.gatekeeperemail.models.EmailTemplateData
import java.util.UUID
import uk.gov.hmrc.gatekeeperemail.models.requests.DevelopersEmailQuery
import uk.gov.hmrc.gatekeeperemail.models.EmailStatus
import org.mongodb.scala.Document
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import java.time.LocalDateTime
import java.time.ZoneOffset
import uk.gov.hmrc.mongo.test.MongoSupport

class DraftEmailSpec extends AsyncHmrcSpec with GuiceOneServerPerSuite with BeforeAndAfterEach with MongoSupport {
  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder = {
    GuiceApplicationBuilder()
      .configure(
        "metrics.jvm" -> false,
        "mongodb.uri" -> s"mongodb://localhost:27017/test-${this.getClass.getSimpleName}"
      )
      .disable(classOf[SchedulerModule])
  }

  val draftEmailRepository = app.injector.instanceOf[DraftEmailRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(draftEmailRepository.collection.drop().toFuture())
    await(draftEmailRepository.ensureIndexes)
  }

  trait Setup {
    val localDateTimeNow = LocalDateTime.now()
    val createDateTimeInstant = localDateTimeNow.toInstant(ZoneOffset.UTC)
    val createDateTimeString = localDateTimeNow.toString()

    val emailUuid = UUID.randomUUID.toString()
    val templateData     = EmailTemplateData("templateId", Map(), false, Map(), None)
    val emailPreferences = DevelopersEmailQuery()
    
    val email = DraftEmail(
      emailUuid,
      templateData,
      "Test Recipient",
      emailPreferences,
      attachmentDetails = Some(List.empty),
      "Test Markdown Email Body",
      "Test Email",
      "Test Subject",
      EmailStatus.FAILED,
      "Test Composed By",
      Some("Test Approved By"),
      createDateTimeInstant,
      emailsCount = 1
    )
  }
  
  "Draft email repository" should {
    "do something" in new Setup {
      await(mongoDatabase.getCollection("draftemails").insertOne(Document(RawEmailTestData.fromDraftEmail(email).toString())).toFuture())

      await(draftEmailRepository.getEmailData(emailUuid))
    }
  }
}
