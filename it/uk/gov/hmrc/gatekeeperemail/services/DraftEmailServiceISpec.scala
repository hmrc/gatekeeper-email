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

package uk.gov.hmrc.gatekeeperemail.services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.mongodb.scala.ReadPreference.primaryPreferred
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport

import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.connectors.DeveloperConnector.RegisteredUser
import uk.gov.hmrc.gatekeeperemail.connectors.{ApmConnector, DeveloperConnector, GatekeeperEmailConnector, GatekeeperEmailRendererConnector}
import uk.gov.hmrc.gatekeeperemail.models._
import uk.gov.hmrc.gatekeeperemail.models.requests.{DevelopersEmailQuery, EmailData, EmailRequest}
import uk.gov.hmrc.gatekeeperemail.repositories.{DraftEmailRepository, SentEmailRepository}
import uk.gov.hmrc.gatekeeperemail.utils.FixedClock

class DraftEmailServiceISpec extends AnyWordSpec with Matchers with BeforeAndAfterEach with MockitoSugar with ArgumentMatchersSugar
    with GuiceOneAppPerSuite with FixedClock with PlayMongoRepositorySupport[DraftEmail] {
  val emailRepository     = repository.asInstanceOf[DraftEmailRepository]
  val sentEmailRepository = serepository.asInstanceOf[SentEmailRepository]

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
  protected def serepository: PlayMongoRepository[SentEmail]         = app.injector.instanceOf[SentEmailRepository]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup {
    val emailPreferences                                             = DevelopersEmailQuery()
    implicit val hc: HeaderCarrier                                   = HeaderCarrier()
    val appConfigMock                                                = mock[AppConfig]
    val developerConnectorMock: DeveloperConnector                   = mock[DeveloperConnector]
    val apmConnectorMock: ApmConnector                               = mock[ApmConnector]
    val emailConnectorMock: GatekeeperEmailConnector                 = mock[GatekeeperEmailConnector]
    val emailRendererConnectorMock: GatekeeperEmailRendererConnector = mock[GatekeeperEmailRendererConnector]
    val underTest                                                    = new DraftEmailService(emailRendererConnectorMock, developerConnectorMock, apmConnectorMock, emailRepository, sentEmailRepository, appConfigMock, clock)
    val users                                                        = List(RegisteredUser("example@example.com", "first name", "last name", true), RegisteredUser("example2@example2.com", "first name2", "last name2", true))
  }

  "saveEmail" should {

    "save the email data into mongodb repo" in new Setup {
      when(emailConnectorMock.sendEmail(*)).thenReturn(Future(200))
      when(developerConnectorMock.fetchVerified()(*)).thenReturn(Future(users))
      when(emailRendererConnectorMock.getTemplatedEmail(*))
        .thenReturn(successful(Right(RenderResult(
          "RGVhciB1c2VyLCBUaGlzIGlzIGEgdGVzdCBtYWls",
          "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA==",
          "from@digital.hmrc.gov.uk",
          "subject",
          ""
        ))))
      val emailRequest      = EmailRequest(emailPreferences, "gatekeeper", EmailData("Test subject", "Dear Mr XYZ, This is test email"), false, Map())
      val email: DraftEmail = await(underTest.persistEmail(emailRequest, "emailUUID"))
      email.htmlEmailBody shouldBe "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA=="
      val fetchedRecords    = await(emailRepository.collection.withReadPreference(primaryPreferred()).find().toFuture())

      fetchedRecords.size shouldBe 1
      fetchedRecords.head.htmlEmailBody shouldBe "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA=="
      fetchedRecords.head.markdownEmailBody shouldBe "RGVhciB1c2VyLCBUaGlzIGlzIGEgdGVzdCBtYWls"

    }
  }
}
