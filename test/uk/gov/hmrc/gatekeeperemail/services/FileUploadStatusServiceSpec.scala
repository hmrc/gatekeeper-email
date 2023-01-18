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

package uk.gov.hmrc.gatekeeperemail.services

import java.util.UUID.randomUUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{FiniteDuration, SECONDS}

import akka.util.Timeout
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.await
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport

import uk.gov.hmrc.gatekeeperemail.models.{Reference, UploadedFailedWithErrors, UploadedSuccessfully}
import uk.gov.hmrc.gatekeeperemail.repositories.{FileUploadStatusRepository, UploadInfo}

class FileUploadStatusServiceSpec extends AnyWordSpec with PlayMongoRepositorySupport[UploadInfo] with Matchers with BeforeAndAfterEach with GuiceOneAppPerSuite {

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()

  override implicit lazy val app: Application = appBuilder.build()
  override protected def repository           = app.injector.instanceOf[FileUploadStatusRepository]
  val t                                       = new FileUploadStatusService(repository)

  override def beforeEach(): Unit = {
    prepareDatabase()
  }

  "MongoBackedUploadProgressTracker" should {
    "coordinate workflow" in {
      val reference      = randomUUID().toString
      val expectedStatus = UploadedSuccessfully("name", "mimeType", "downloadUrl", Some(123), "http://aws.s3.object-store-url")

      implicit val timeout = Timeout(FiniteDuration(20, SECONDS))
      await(t.requestUpload(reference))
      await(t.registerUploadResult(reference, UploadedSuccessfully("name", "mimeType", "downloadUrl", Some(123), "http://aws.s3.object-store-url")))
      await(t.getUploadResult(Reference(reference))).get.status shouldBe expectedStatus
    }

    "update status as failedWithErrors workflow" in {
      val reference      = randomUUID().toString
      val expectedStatus = UploadedFailedWithErrors("VIRUS", "found Virus", "1233", reference)

      implicit val timeout = Timeout(FiniteDuration(20, SECONDS))
      await(t.requestUpload(reference))
      await(t.registerUploadResult(reference, UploadedFailedWithErrors("VIRUS", "found Virus", "1233", reference)))
      await(t.getUploadResult(Reference(reference))).get.status shouldBe expectedStatus
    }
  }
}
