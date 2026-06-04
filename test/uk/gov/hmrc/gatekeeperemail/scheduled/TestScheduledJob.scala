/*
 * Copyright 2026 HM Revenue & Customs
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

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import java.util.concurrent.CountDownLatch
import scala.concurrent.Promise

class TestScheduledJob(startedMarker: CountDownLatch) extends ScheduledJob {
  override lazy val initialDelay: FiniteDuration = 1.seconds
  override lazy val interval: FiniteDuration     = 5.seconds
  def name: String                               = "TestScheduledJob"

  override def execute(implicit ec: ExecutionContext): Future[Result] = {
    startedMarker.countDown()
    Future.successful(Result("done"))
  }

  var isRunning: Future[Boolean] = Future.successful(false)
}

class ShutdownDelayedScheduledJob(startedMarker: CountDownLatch, promise: Promise[String]) extends ScheduledJob {
  override lazy val initialDelay: FiniteDuration = 1.seconds
  override lazy val interval: FiniteDuration     = 30.seconds
  def name: String                               = "StopDelayedScheduledJob"

  override def execute(implicit ec: ExecutionContext): Future[Result] = {
    startedMarker.countDown()
    promise.future.map(Result(_))
  }
}
