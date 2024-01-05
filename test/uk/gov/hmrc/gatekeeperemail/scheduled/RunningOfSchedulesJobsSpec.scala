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

import scala.concurrent.duration.{Deadline, DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

import akka.actor.Cancellable
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Minute, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerTest

import play.api.Application
import play.api.inject.ApplicationLifecycle
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}

import uk.gov.hmrc.gatekeeperemail.utils.HmrcSpec

class RunningOfSchedulesJobsSpec extends HmrcSpec with ScalaFutures with GuiceOneAppPerTest with BeforeAndAfterEach {

  override def fakeApplication() =
    new GuiceApplicationBuilder().configure(
      "metrics.jvm"     -> false,
      "metrics.enabled" -> false
    )
      .build()

  trait Setup extends TestCase {

    val subject = new RunningOfScheduledJobs {
      override implicit val ec: ExecutionContext              = ExecutionContext.Implicits.global
      override val application: Application                   = fakeApplication()
      override val scheduledJobs: Seq[ScheduledJob]           = Seq(new TestScheduledJob)
      override val applicationLifecycle: ApplicationLifecycle = fakeApplication().injector.instanceOf[ApplicationLifecycle]
    }
  }

  "When stopping the app, the scheduled job runner" should {
    "cancel all of the scheduled jobs" in new TestCase {
      private val testApp = fakeApplication()
      private val runner  = new RunningOfScheduledJobs {
        override lazy val ec: ExecutionContext                       = ExecutionContext.Implicits.global
        override lazy val applicationLifecycle: ApplicationLifecycle = testApp.injector.instanceOf[ApplicationLifecycle]
        override lazy val scheduledJobs: Seq[LockedScheduledJob]     = Seq.empty
        override lazy val application: Application                   = testApp
      }
      runner.cancellables = Seq(new StubCancellable, new StubCancellable)

      every(runner.cancellables) should not be Symbol("cancelled")
      await(testApp.stop())
      every(runner.cancellables) shouldBe Symbol("cancelled")
    }

    "block while scheduled jobs are still running" in new TestCase {
      private val testApp = fakeApplication()
      val stoppableJob    = new TestScheduledJob() {
        override def name: String = "StoppableJob"
      }
      private val runner  = new RunningOfScheduledJobs {
        override lazy val ec: ExecutionContext                       = ExecutionContext.Implicits.global
        override lazy val applicationLifecycle: ApplicationLifecycle = testApp.injector.instanceOf[ApplicationLifecycle]
        override lazy val scheduledJobs: Seq[ScheduledJob]           = Seq(stoppableJob)
        override lazy val application: Application                   = testApp
      }

      stoppableJob.isRunning = Future.successful(true)

      val deadline: Deadline = 5000.milliseconds.fromNow
      while (deadline.hasTimeLeft()) {
        /* Intentionally burning CPU cycles for fixed period */
      }

      val stopFuture = testApp.stop()
      stopFuture should not be Symbol("completed")

      stoppableJob.isRunning = Future.successful(false)
      eventually(timeout(Span(1, Minute))) { stopFuture shouldBe Symbol("completed") }
    }
  }

  trait TestCase {

    class StubbedScheduler extends akka.actor.Scheduler {

      override def scheduleWithFixedDelay(
          initialDelay: FiniteDuration,
          delay: FiniteDuration
        )(
          runnable: Runnable
        )(implicit executor: ExecutionContext
        ): Cancellable = new Cancellable {
        override def cancel(): Boolean    = true
        override def isCancelled: Boolean = false
      }
      def maxFrequency: Double = 1

      def scheduleOnce(delay: FiniteDuration, runnable: Runnable)(implicit executor: ExecutionContext): Cancellable = new Cancellable {
        override def cancel(): Boolean    = true
        override def isCancelled: Boolean = false
      }

      override def schedule(initialDelay: FiniteDuration, interval: FiniteDuration, runnable: Runnable)(implicit executor: ExecutionContext) = ???
    }

    class TestScheduledJob extends ScheduledJob {
      override lazy val initialDelay: FiniteDuration = 2.seconds
      override lazy val interval: FiniteDuration     = 3.seconds
      def name: String                               = "TestScheduledJob"
      def isExecuted: Boolean                        = true

      override def execute(implicit ec: ExecutionContext): Future[Result] = Future.successful(Result("done"))
      var isRunning: Future[Boolean]                                      = Future.successful(false)
    }
    val testScheduledJob = new TestScheduledJob

    class StubCancellable extends Cancellable {
      var isCancelled = false

      def cancel(): Boolean = {
        isCancelled = true
        isCancelled
      }
    }
  }

}
