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

package uk.gov.hmrc.gatekeeperemail.scheduled

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext}

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Minute, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerTest

import play.api.Application
import play.api.inject.ApplicationLifecycle
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.apiplatform.modules.common.utils.HmrcSpec
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import scala.concurrent.Promise
import scala.util.Success

class ScheduledJobsRunnerSpec extends HmrcSpec with ScalaFutures with GuiceOneAppPerTest with BeforeAndAfterEach {

  override def fakeApplication() =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"     -> false,
        "metrics.enabled" -> false
      )
      .disable[SchedulerModule]
      .build()

  "ScheduledJobsRunner" should {
    "Invoke execute method on jobs after small delay" in {
      val cdl = new CountDownLatch(2)
      val testScheduledJob1     = new TestScheduledJob(cdl)
      val testScheduledJob2     = new TestScheduledJob(cdl)

      val testApp              = fakeApplication()
      val applicationLifecycle = testApp.injector.instanceOf[ApplicationLifecycle]

      ScheduledJobsRunner(testApp, applicationLifecycle, ScheduledJobs(List(testScheduledJob1, testScheduledJob2)))

      // Both jobs should be executed quickly
      cdl.await(5, TimeUnit.SECONDS) shouldBe true

      await(testApp.stop())
    }
  
    "When stopping the app, the scheduled job runner should cancel all of the scheduled jobs" in {
      val cdl = new CountDownLatch(2)
      val testScheduledJob1     = new TestScheduledJob(cdl)
      val testScheduledJob2     = new TestScheduledJob(cdl)

      val testApp              = fakeApplication()
      val applicationLifecycle = testApp.injector.instanceOf[ApplicationLifecycle]

      val runner               = ScheduledJobsRunner(testApp, applicationLifecycle, ScheduledJobs(List(testScheduledJob1, testScheduledJob2)))

      every(runner.cancellables) should not be Symbol("cancelled")
      await(testApp.stop())
      every(runner.cancellables) shouldBe Symbol("cancelled")
    }

    "block while scheduled jobs are still running" in {
      val testApp              = fakeApplication()
      val applicationLifecycle = testApp.injector.instanceOf[ApplicationLifecycle]

      val startedMarker = new CountDownLatch(1)
      val jobCompleter = Promise[String]
      val job = new ShutdownDelayedScheduledJob(startedMarker, jobCompleter)

      val runner               = ScheduledJobsRunner(testApp, applicationLifecycle, ScheduledJobs(List(job)))

      // Ensure we are running execute block
      startedMarker.await(5, TimeUnit.SECONDS) shouldBe true

      val stopFuture = testApp.stop()
      // When we stop the app it should wait for the runnning job to complete
      stopFuture should not be Symbol("completed")

      // Complete the job
      jobCompleter.complete(Success("Done"))

      // Check everything is shutdown
      eventually(timeout(Span(1, Minute))) { stopFuture shouldBe Symbol("completed") }
      every(runner.cancellables) shouldBe Symbol("cancelled")
    }
  }
}
