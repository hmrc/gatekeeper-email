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

package uk.gov.hmrc.gatekeeperemail.services

import akka.stream.Materializer
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.mongodb.scala.ReadPreference.primaryPreferred
import org.mongodb.scala.bson.BsonValue
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.gatekeeperemail.connectors.{GatekeeperEmailConnector, GatekeeperEmailRendererConnector}
import uk.gov.hmrc.gatekeeperemail.models.{Email, EmailData, EmailRequest, RenderResult}
import uk.gov.hmrc.gatekeeperemail.repositories.EmailRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful

class EmailServiceISpec extends AnyWordSpec with Matchers with BeforeAndAfterEach with MockitoSugar with ArgumentMatchersSugar
  with GuiceOneAppPerSuite with PlayMongoRepositorySupport[Email] {
  val emailRepository = repository.asInstanceOf[EmailRepository]

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

  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val emailConnectorMock: GatekeeperEmailConnector = mock[GatekeeperEmailConnector]
    val emailRendererConnectorMock: GatekeeperEmailRendererConnector = mock[GatekeeperEmailRendererConnector]
    val underTest = new EmailService(emailConnectorMock, emailRendererConnectorMock, emailRepository)
  }

  "saveEmail" should {

    "save the email data into mongodb repo" in new Setup {
      when(emailConnectorMock.sendEmail(*)).thenReturn(Future(200))
      when(emailRendererConnectorMock.getTemplatedEmail(*))
        .thenReturn(successful(Right(RenderResult("RGVhciB1c2VyLCBUaGlzIGlzIGEgdGVzdCBtYWls",
          "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA==", "from@digital.hmrc.gov.uk", "subject", ""))))
      val emailRequest = EmailRequest(List("test@digital.hmrc.gov.uk"), "gatekeeper",
        EmailData("Recipient Title", "Test subject", "Dear Mr XYZ, This is test email"), false, Map())
      val insertedId: BsonValue = await(underTest.sendAndPersistEmail(emailRequest))
      insertedId.asObjectId().getValue should not be (null)
      val fetchedRecords = await(emailRepository.collection.withReadPreference(primaryPreferred).find().toFuture())

      fetchedRecords.size shouldBe 1
      fetchedRecords.head.htmlEmailBody shouldBe Some("PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA==")
      fetchedRecords.head.markdownEmailBody shouldBe "RGVhciB1c2VyLCBUaGlzIGlzIGEgdGVzdCBtYWls"

    }
  }
}
