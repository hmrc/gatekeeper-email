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
import uk.gov.hmrc.gatekeeperemail.repository.{FileUploadStatusRepository, UploadInfo}
import uk.gov.hmrc.mongo.{Awaiting, MongoConnector, MongoSpecSupport}
import uk.gov.hmrc.gatekeeperemail.connectors.Reference
import akka.actor.ActorSystem
import org.mockito.MockitoSugar.mock
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.gatekeeperemail.models.{UploadId, UploadedSuccessfully}


class FileUploadStatusServiceSpec extends WordSpec with MongoSpecSupport with Awaiting with Matchers with BeforeAndAfterEach {

  val system = mock[ActorSystem]
  val mongoComponent = new MongoComponent(mongoUri)
  val repo = new FileUploadStatusRepository(mongoComponent)
  val t = new FileUploadStatusService(repo, system)

  override def beforeEach(): Unit = {
    await(repo.removeAll())
  }

  "MongoBackedUploadProgressTracker" should {
    "coordinate workflow" in {
      val reference = Reference("reference")
      val id = UploadId("upload-id")
      val str = "61adf36cda0000130b757df9".getBytes()
      val expectedStatus = UploadedSuccessfully("name","mimeType","downloadUrl",Some(123))

      await(t.requestUpload(id, reference))
      await(t.registerUploadResult(reference, UploadedSuccessfully("name","mimeType","downloadUrl",Some(123))))
      await(t.getUploadResult(id)).get.status shouldBe expectedStatus
    }
  }
}

class MongoComponent(mongoConnectionUri: String) extends ReactiveMongoComponent {
  override def mongoConnector: MongoConnector = MongoConnector(mongoConnectionUri)
}