/*
 * Copyright 2021 HM Revenue & Customs
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

import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import play.api.test.Helpers.await
import uk.gov.hmrc.gatekeeperemail.repository.{FileUploadStatusRepository, UploadInfo}
import uk.gov.hmrc.gatekeeperemail.models.Reference
import akka.util.Timeout
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.gatekeeperemail.models.{UploadId, UploadedSuccessfully}
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport

import java.util.UUID.randomUUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{FiniteDuration, SECONDS}

class FileUploadStatusServiceSpec extends AnyWordSpec with PlayMongoRepositorySupport[UploadInfo] with
  Matchers with BeforeAndAfterEach with GuiceOneAppPerSuite {

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()

  override implicit lazy val app: Application = appBuilder.build()
  override protected def repository = app.injector.instanceOf[FileUploadStatusRepository]
  val t = new FileUploadStatusService(repository)

  override def beforeEach(): Unit = {
    prepareDatabase()
  }

  "MongoBackedUploadProgressTracker" should {
    "coordinate workflow" in {
      val reference = Reference(randomUUID().toString)
      val id = UploadId(randomUUID)
      val str = "61adf36cda0000130b757df9".getBytes()
      val expectedStatus = UploadedSuccessfully("name","mimeType","downloadUrl",Some(123))

      implicit val timeout = Timeout(FiniteDuration(20, SECONDS))
      await(t.requestUpload(id, reference))
      await(t.registerUploadResult(reference, UploadedSuccessfully("name","mimeType","downloadUrl",Some(123))))
      await(t.getUploadResult(reference)).get.status shouldBe expectedStatus
    }
  }
}
