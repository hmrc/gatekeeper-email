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
import uk.gov.hmrc.gatekeeperemail.models.EmailStatus.IN_PROGRESS
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
      "test subject", "SENT", "composedBy", Some("approvedBy"), LocalDateTime.now())
    val sentEmail = SentEmail(createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now(), emailUuid = UUID.randomUUID(),
      firstName = "first", lastName = "last", recipient = "first.last@digital.hmrc.gov.uk", status = IN_PROGRESS,
      failedCount = 0)

    when(emailRendererConnectorMock.getTemplatedEmail(*))
      .thenReturn(successful(Right(RenderResult("RGVhciB1c2VyLCBUaGlzIGlzIGEgdGVzdCBtYWls",
        "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA==", "from@digital.hmrc.gov.uk", "subject", ""))))
  }

  "handleEmailSendingFailed" should {
    "increment failed count when maximum fail count not reached" in new Setup {
      when(sentEmailRepositoryMock.incrementFailedCount(*)).thenReturn(Future(sentEmail))

      underTest.handleEmailSendingFailed(sentEmail)

      verify(sentEmailRepositoryMock).incrementFailedCount(sentEmail)
    }

  "mark email as failed when maximum fail count is reached" in new Setup {
    val sentEmailWithFailedCount = sentEmail.copy(failedCount = 4)
      when(sentEmailRepositoryMock.incrementFailedCount(*)).thenReturn(Future(sentEmail))

      underTest.handleEmailSendingFailed(sentEmailWithFailedCount)

      verify(sentEmailRepositoryMock).markFailed(sentEmailWithFailedCount)
      verifyNoMoreInteractions(sentEmailRepositoryMock)
    }
  }

  "findAndSendNextEmail" should {
    "successfully find a draft email and send it" in new Setup {
      when(draftEmailServiceMock.fetchEmail(sentEmail.emailUuid.toString)).thenReturn(Future(draftEmail))
      when(emailConnectorMock.sendEmail(*)).thenReturn(Future(200))

      val result = await(underTest.findAndSendNextEmail(sentEmail))

      result shouldBe 200
    }
  }
}
