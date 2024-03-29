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

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.mongo.lock.MongoLockRepository

import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.services.SentEmailService
import uk.gov.hmrc.gatekeeperemail.util.ApplicationLogger

@Singleton
class EmailSendingJob @Inject() (appConfig: AppConfig, override val mongoLockRepository: MongoLockRepository, sentEmailService: SentEmailService)
    extends LockedScheduledJob with ApplicationLogger {

  override def name: String = "EmailSendingJob"

  private val jobConfig = appConfig.scheduledJobConfig(name)

  override def initialDelay: FiniteDuration = jobConfig.initialDelay

  override def interval: FiniteDuration = jobConfig.interval

  override def enabled: Boolean = jobConfig.enabled

  override def executeInLock(implicit ec: ExecutionContext): Future[Result] = {
    sentEmailService.sendNextPendingEmail.map(done => {
      // If an email has been sent then log a message out to confirm this.
      if (done == 1) {
        logger.info(s"${name} has successfully sent the next pending email.")
      }

      Result(done.toString)
    })
  }
}
