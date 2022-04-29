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

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.services.EmailService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailSendingJob @Inject()(appConfig: AppConfig, override val lockService: LockService,
                                emailService: EmailService)
  extends LockedScheduledJob {

//  override val releaseLockAfter: Duration = Duration.standardSeconds(appConfig.retryJobLockDuration.toSeconds)

  override def name: String = "EmailSendingJob"

  override def initialDelay: FiniteDuration = appConfig.initialDelay.asInstanceOf[FiniteDuration]

  override def interval: FiniteDuration = appConfig.interval.asInstanceOf[FiniteDuration]

  override def executeInLock(implicit ec: ExecutionContext): Future[Result] = {
    emailService.sendEmails.map(done => Result(done.toString))
  }
}