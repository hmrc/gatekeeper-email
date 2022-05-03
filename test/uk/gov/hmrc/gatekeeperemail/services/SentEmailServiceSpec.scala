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

import java.time.LocalDateTime
import java.util.UUID

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.gatekeeperemail.connectors.{GatekeeperEmailConnector, GatekeeperEmailRendererConnector}
import uk.gov.hmrc.gatekeeperemail.models.EmailStatus._
import uk.gov.hmrc.gatekeeperemail.models._
import uk.gov.hmrc.gatekeeperemail.repositories.{DraftEmailRepository, SentEmailRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful

class SentEmailServiceSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar with ArgumentMatchersSugar {

  trait Setup {
    val draftEmailRepositoryMock: DraftEmailRepository = mock[DraftEmailRepository]
    val draftEmailServiceMock: EmailService = mock[EmailService]
    val sentEmailRepositoryMock: SentEmailRepository = mock[SentEmailRepository]
    val emailConnectorMock: GatekeeperEmailConnector = mock[GatekeeperEmailConnector]
    val emailRendererConnectorMock: GatekeeperEmailRendererConnector = mock[GatekeeperEmailRendererConnector]
    val underTest = new SentEmailService(emailConnectorMock, draftEmailServiceMock, sentEmailRepositoryMock)
    val templateData = EmailTemplateData("templateId", Map(), false, Map(), None)
    val users = List(User("example@example.com", "first name", "last name", true),
      User("example2@example2.com", "first name2", "last name2", true))
    val draftEmail = DraftEmail("emailId-123", templateData, "DL Team",
      users, None, "markdownEmailBody", "Test email",
      "test subject", SENT, "composedBy", Some("approvedBy"), LocalDateTime.now())
    val sentEmail = SentEmail(createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now(), emailUuid = UUID.randomUUID(),
      firstName = "first", lastName = "last", recipient = "first.last@digital.hmrc.gov.uk", status = PENDING,
      failedCount = 0)

    when(emailRendererConnectorMock.getTemplatedEmail(*))
      .thenReturn(successful(Right(RenderResult("RGVhciB1c2VyLCBUaGlzIGlzIGEgdGVzdCBtYWls",
        "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA==", "from@digital.hmrc.gov.uk", "subject", ""))))
  }

  "sendEmails" should {
    "mark email as sent when email connector receives success response" in new Setup {
      when(sentEmailRepositoryMock.findNextEmailToSend).thenReturn(Future(Some(sentEmail)))
      when(draftEmailServiceMock.fetchEmail(sentEmail.emailUuid.toString)).thenReturn(Future(draftEmail))
      when(emailConnectorMock.sendEmail(*)).thenReturn(Future(200))

      await(underTest.sendEmails)

      verify(sentEmailRepositoryMock).findNextEmailToSend
      verify(draftEmailServiceMock).fetchEmail(sentEmail.emailUuid.toString)
      verify(sentEmailRepositoryMock).markSent(sentEmail)
    }

    "increment failed count when maximum fail count not reached" in new Setup {
      when(sentEmailRepositoryMock.incrementFailedCount(*)).thenReturn(Future(sentEmail))
      when(sentEmailRepositoryMock.findNextEmailToSend).thenReturn(Future(Some(sentEmail)))
      when(draftEmailServiceMock.fetchEmail(sentEmail.emailUuid.toString)).thenReturn(Future(draftEmail))
      when(emailConnectorMock.sendEmail(*)).thenReturn(Future(400))

      await(underTest.sendEmails)

      verify(draftEmailServiceMock).fetchEmail(sentEmail.emailUuid.toString)
      verify(sentEmailRepositoryMock).incrementFailedCount(sentEmail)
    }

    "mark as failed when maximum fail count reached" in new Setup {
      val emailToSend   = sentEmail.copy(failedCount = 4)
      when(sentEmailRepositoryMock.markFailed(emailToSend)).thenReturn(Future(emailToSend))
      when(sentEmailRepositoryMock.findNextEmailToSend).thenReturn(Future(Some(emailToSend)))
      when(draftEmailServiceMock.fetchEmail(emailToSend.emailUuid.toString)).thenReturn(Future(draftEmail))
      when(emailConnectorMock.sendEmail(*)).thenReturn(Future(400))

      await(underTest.sendEmails)

      verify(draftEmailServiceMock).fetchEmail(emailToSend.emailUuid.toString)
      verify(sentEmailRepositoryMock).markFailed(emailToSend)
    }
  }
}
