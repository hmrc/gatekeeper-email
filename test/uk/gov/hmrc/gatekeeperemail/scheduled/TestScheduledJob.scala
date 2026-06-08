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

import java.util.concurrent.CountDownLatch
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future, Promise}

class SequencingScheduledJob(
    val initialDelay: FiniteDuration = 1.seconds,
    jobCompleter: Promise[String] = Promise.successful("Done")
) extends ScheduledJob {
  override lazy val interval: FiniteDuration = 1.hour
  def name: String                           = "SequencingScheduledJob1"

  private val startedMarker   = CountDownLatch(1)
  private val completedMarker = CountDownLatch(1)

  def awaitStarted(timeout: FiniteDuration): Boolean   = {
    startedMarker.await(timeout.length, timeout.unit)
  }
  def awaitCompleted(timeout: FiniteDuration): Boolean = {
    completedMarker.await(timeout.length, timeout.unit)
  }

  override def execute(implicit ec: ExecutionContext): Future[Result] = {
    startedMarker.countDown()
    val future = jobCompleter.future.map(Result(_))
    future.onComplete { case _ =>
      completedMarker.countDown()
    }
    future
  }
}
