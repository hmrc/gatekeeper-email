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
import com.mongodb.client.result.InsertOneResult
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.mongodb.scala.bson.{BsonNumber, BsonValue}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.gatekeeperemail.connectors.{GatekeeperEmailConnector, GatekeeperEmailRendererConnector}
import uk.gov.hmrc.gatekeeperemail.models.{Email, EmailData, EmailRequest, EmailTemplateData, RenderResult, User}
import uk.gov.hmrc.gatekeeperemail.repositories.EmailRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful

class EmailServiceSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar with ArgumentMatchersSugar {
  implicit val mat: Materializer = app.injector.instanceOf[Materializer]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val emailRepositoryMock: EmailRepository = mock[EmailRepository]
    val emailConnectorMock: GatekeeperEmailConnector = mock[GatekeeperEmailConnector]
    val emailRendererConnectorMock: GatekeeperEmailRendererConnector = mock[GatekeeperEmailRendererConnector]
    val underTest = new EmailService(emailConnectorMock, emailRendererConnectorMock, emailRepositoryMock)
    val templateData = EmailTemplateData("templateId", Map(), false, Map(), None)
    val users = List(User("example@example.com", "first name", "last name", true),
      User("example2@example2.com", "first name2", "last name2", true))
    val email = Email("emailId-123", templateData, "DL Team",
      users, None, "markdownEmailBody", "This is test email",
      "test subject", "composedBy", Some("approvedBy"), DateTime.now(UTC))
    when(emailRendererConnectorMock.getTemplatedEmail(*))
      .thenReturn(successful(Right(RenderResult("RGVhciB1c2VyLCBUaGlzIGlzIGEgdGVzdCBtYWls",
        "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA==", "from@digital.hmrc.gov.uk", "subject", ""))))

  }

  "saveEmail" should {

    "save the email data into mongodb repo" in new Setup {
      when(emailConnectorMock.sendEmail(*)).thenReturn(Future(200))
      when(emailRepositoryMock.persist(*)).thenReturn(Future(InsertOneResult.acknowledged(BsonNumber(1))))
      val emailRequest = EmailRequest(users, "gatekeeper",
        EmailData("Test subject", "Dear Mr XYZ, This is test email"), false, Map())
      val emailFromMongo: Email = await(underTest.persistEmail(emailRequest, "emailUUID"))
      emailFromMongo.subject shouldBe "Test subject"
      emailFromMongo.htmlEmailBody shouldBe "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA=="
      emailFromMongo.templateData.templateId shouldBe "gatekeeper"
    }

    "save the email data into mongodb repo even when fails to send" in new Setup {
      when(emailConnectorMock.sendEmail(*)).thenReturn(Future(400))
      when(emailRepositoryMock.persist(*)).thenReturn(Future(InsertOneResult.acknowledged(BsonNumber(1))))
      val emailRequest = EmailRequest(users, "gatekeeper",
        EmailData("Test subject2", "Dear Mr XYZ, This is test email"), false, Map())
      val emailFromMongo: Email = await(underTest.persistEmail(emailRequest, "emailUUID"))
      emailFromMongo.subject shouldBe "Test subject2"
      emailFromMongo.htmlEmailBody shouldBe "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA=="
      emailFromMongo.templateData.templateId shouldBe "gatekeeper"
    }
  }

  "updateEmail" should {

    "update the email data into mongodb repo" in new Setup {
      when(emailConnectorMock.sendEmail(*)).thenReturn(Future(200))
      when(emailRepositoryMock.updateEmail(*)).thenReturn(Future(email))
      val emailRequest = EmailRequest(users, "gatekeeper",
        EmailData("Test subject", "Dear Mr XYZ, This is test email"), false, Map())
      val emailFromMongo: Email = await(underTest.updateEmail(emailRequest, "emailUUID"))
      emailFromMongo.subject shouldBe "Test subject"
      emailFromMongo.htmlEmailBody shouldBe "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA=="
      emailFromMongo.templateData.templateId shouldBe "gatekeeper"
    }

    "update the email data into mongodb repo even when fails to send" in new Setup {
      when(emailConnectorMock.sendEmail(*)).thenReturn(Future(400))
      when(emailRepositoryMock.updateEmail(*)).thenReturn(Future(email))
      val emailRequest = EmailRequest(users, "gatekeeper",
        EmailData("Test subject2", "Dear Mr XYZ, This is test email"), false, Map())
      val emailFromMongo: Email = await(underTest.updateEmail(emailRequest, "emailUUID"))
      emailFromMongo.subject shouldBe "Test subject2"
      emailFromMongo.htmlEmailBody shouldBe "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA=="
      emailFromMongo.templateData.templateId shouldBe "gatekeeper"
    }
  }
}
