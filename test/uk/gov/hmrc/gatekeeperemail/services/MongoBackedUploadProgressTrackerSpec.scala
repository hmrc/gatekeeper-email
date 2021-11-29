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
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.gatekeeperemail.model.{UploadId, UploadedSuccessfully}
import uk.gov.hmrc.gatekeeperemail.repository.UserSessionRepository
import uk.gov.hmrc.mongo.{Awaiting, MongoConnector, MongoSpecSupport}
import uk.gov.hmrc.gatekeeperemail.connectors.Reference


class MongoBackedUploadProgressTrackerSpec extends WordSpec with MongoSpecSupport with Awaiting with Matchers with BeforeAndAfterEach {

  val mongoComponent = new MongoComponent(mongoUri)
  val repo = new UserSessionRepository(mongoComponent)
  val t = new MongoBackedUploadProgressTracker(repo)

  override def beforeEach(): Unit = {
    await(repo.removeAll())
  }

  "MongoBackedUploadProgressTracker" should {
    "coordinate workflow" in {
      val reference = Reference("reference")
      val id = UploadId("upload-id")
      val expectedStatus = UploadedSuccessfully("name", "mimeType", "downloadUrl", size = Some(123))

      await(t.requestUpload(id, reference))
      await(t.registerUploadResult(reference, expectedStatus))
      await(t.getUploadResult(id)) shouldBe Some(expectedStatus)
    }
  }
}

class MongoComponent(mongoConnectionUri: String) extends ReactiveMongoComponent {
  override def mongoConnector: MongoConnector = MongoConnector(mongoConnectionUri)
}