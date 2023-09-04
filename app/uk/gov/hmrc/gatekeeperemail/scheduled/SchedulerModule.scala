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

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

import com.google.inject.AbstractModule

import play.api.inject.{ApplicationLifecycle, Binding, Module}
import play.api.{Application, Configuration, Environment}
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}

class SchedulerModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[Scheduler]).asEagerSingleton()
  }
}

@Singleton
class Scheduler @Inject() (
    override val applicationLifecycle: ApplicationLifecycle,
    override val application: Application,
    emailSendingJob: EmailSendingJob
  )(
    override implicit val ec: ExecutionContext
  ) extends RunningOfScheduledJobs {
  override lazy val scheduledJobs: Seq[ScheduledJob] = Seq(emailSendingJob)
}

class SchedulerPlayModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[LockService].toProvider[LockServiceProvider]
    )
  }
}

@Singleton
class LockServiceProvider @Inject() (mongoLockRepository: MongoLockRepository) extends Provider[LockService] {
  override def get(): LockService = LockService(mongoLockRepository, lockId = "send-email-lock", ttl = 1.hour)
}
