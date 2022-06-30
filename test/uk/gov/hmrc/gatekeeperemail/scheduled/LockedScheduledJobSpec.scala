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

package uk.gov.hmrc.gatekeeperemail.scheduled

import org.mockito.scalatest.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.services.SentEmailService
import uk.gov.hmrc.mongo.lock.LockService
import play.api.test.Helpers.{await, defaultAwaitTimeout}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class LockedScheduledJobSpec extends AnyWordSpec with Matchers with ScalaFutures with GuiceOneAppPerTest with MockitoSugar
    with BeforeAndAfterEach {

  override def fakeApplication() =
    new GuiceApplicationBuilder().configure(
      "metrics.jvm" -> false,
      "metrics.enabled" -> false
    )
      .build()

  trait Setup {
    val mockSentEmailSservice = mock[SentEmailService]
    val mockLockService = mock[LockService]
    val mockAppConfig = mock[AppConfig]
    val subject = new EmailSendingJob(mockAppConfig, mockLockService, mockSentEmailSservice)
  }

  "ExclusiveScheduledJob" should {

    "back off when Mongo lock cannot be obtained" in new Setup {
      when(mockLockService.withLock(*)(*)).thenReturn(Future.successful(None))
      when(mockSentEmailSservice.sendNextPendingEmail).thenReturn(Future(0))

      val result = await(subject.execute)

      result.message shouldBe "Job named EmailSendingJob cannot acquire Mongo lock, not running"
      verify(mockLockService).withLock(*)(*)
      verify(mockSentEmailSservice).sendNextPendingEmail
    }

  "execute in lock when Mongo lock can be obtained" in new Setup {
      when(mockLockService.withLock[subject.Result](*)(*)).thenReturn(Future.successful(Some(subject.Result("OK"))))
      when(mockSentEmailSservice.sendNextPendingEmail).thenReturn(Future(1))

      val result = await(subject.execute)

      result.message shouldBe "Job named EmailSendingJob ran, and completed, with result OK"
      verify(mockLockService).withLock(*)(*)
      verify(mockSentEmailSservice).sendNextPendingEmail
    }
  }
}
