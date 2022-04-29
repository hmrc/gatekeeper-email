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

import com.google.inject.AbstractModule
import javax.inject.{Inject, Provider, Singleton}
import play.api.inject.{ApplicationLifecycle, Binding, Module}
import play.api.{Application, Configuration, Environment}
import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class SchedulerModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[Scheduler]).asEagerSingleton()
  }
}

@Singleton
class Scheduler @Inject()(override val applicationLifecycle: ApplicationLifecycle,
                          override val application: Application,
                          emailSendingJob: EmailSendingJob,
                          appConfig: AppConfig)
                         (override implicit val ec: ExecutionContext) extends RunningOfScheduledJobs {
  override lazy val scheduledJobs: Seq[LockedScheduledJob] = Seq(emailSendingJob)
}

class SchedulerPlayModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[LockService].toProvider[LockServiceProvider]
    )
  }
}

@Singleton
class LockClient @Inject()(mongoLockRepository: MongoLockRepository) {
  val myLock = LockService(mongoLockRepository, lockId = "my-lock", ttl = 1.hour)
}

@Singleton
class LockServiceProvider @Inject()(mongoLockRepository: MongoLockRepository) extends Provider[LockService] {
  override def get(): LockService = LockService(mongoLockRepository, lockId = "my-lock", ttl = 1.hour)
}
/*
class SchedulerPlayModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[LockRepository].toProvider[LockRepositoryProvider]
    )
  }
}

@Singleton
class LockRepositoryProvider @Inject()(mongoComponent: ReactiveMongoComponent) extends Provider[LockRepository] {
  override def get(): LockRepository = new LockRepository()(mongoComponent.mongoConnector.db)
}
*/