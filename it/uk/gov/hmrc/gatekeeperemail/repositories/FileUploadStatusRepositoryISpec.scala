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

package uk.gov.hmrc.gatekeeperemail.repositories

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{FiniteDuration, SECONDS}

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.util.Timeout
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport

import uk.gov.hmrc.gatekeeperemail.common.AsyncHmrcSpec
import uk.gov.hmrc.gatekeeperemail.models._

class FileUploadStatusRepositoryISpec
    extends AsyncHmrcSpec with BeforeAndAfterEach with BeforeAndAfterAll with OptionValues
    with PlayMongoRepositorySupport[UploadInfo] with Matchers with GuiceOneAppPerSuite with FixedClock {

  implicit var s: ActorSystem   = ActorSystem("test")
  implicit var m: Materializer  = Materializer(s)
  implicit val timeOut: Timeout = Timeout(FiniteDuration(20, SECONDS))

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()

  override implicit lazy val app: Application = appBuilder.build()

  override val repository = app.injector.instanceOf[FileUploadStatusRepository]

  override def beforeEach(): Unit = {
    prepareDatabase()
  }

  override protected def afterAll(): Unit = {
    prepareDatabase()
  }

  "save" should {
    "create a file upload status and retrieve it from database" in {
      val fileReference = Reference(UUID.randomUUID().toString)
      val fileStatus    = UploadInfo(fileReference, InProgress, instant)
      await(repository.requestUpload(fileStatus))

      val retrieved = await(repository.findByUploadId(fileReference))

      retrieved.value.status shouldBe fileStatus.status
      retrieved.value.reference shouldBe fileStatus.reference
      retrieved.value.createDateTime should equal(fileStatus.createDateTime)
    }
  }

  "update a fileStatus to success" in {
    val fileReference = Reference(UUID.randomUUID().toString)
    val fileStatus    = UploadInfo(fileReference, InProgress, instant)
    await(repository.requestUpload(fileStatus))

    val retrieved = await(repository.findByUploadId(fileReference))

    retrieved.value.status shouldBe fileStatus.status
    retrieved.value.reference shouldBe fileStatus.reference
    retrieved.value.createDateTime should equal(fileStatus.createDateTime)

    val updated = fileStatus.copy(status = UploadedSuccessfully("abc.jpeg", "jpeg", "http://s3/abc.jpeg", Some(234), "http://aws.object-url"))
    await(repository.updateStatus(reference = fileReference, UploadedSuccessfully("abc.jpeg", "jpeg", "http://s3/abc.jpeg", Some(234), "http://aws.object-url")))

    val fetch = await(repository.findByUploadId(fileReference).map(_.get))
    fetch.status shouldBe updated.status
    fetch.reference shouldBe updated.reference
    fetch.createDateTime should equal(updated.createDateTime)

  }

  "update a fileStatus to failedwithErrors" in {
    val fileReference = Reference(UUID.randomUUID().toString)
    val fileStatus    = UploadInfo(fileReference, InProgress, instant)
    await(repository.requestUpload(fileStatus))

    val retrieved = await(repository.findByUploadId(fileReference))

    retrieved.value.status shouldBe fileStatus.status
    retrieved.value.reference shouldBe fileStatus.reference
    retrieved.value.createDateTime should equal(fileStatus.createDateTime)

    val updated = fileStatus.copy(status = UploadedFailedWithErrors("VIRUS", "found Virus", "1233", fileReference.value))

    await(repository.updateStatus(reference = fileReference, UploadedFailedWithErrors("VIRUS", "found Virus", "1233", fileReference.value)))
    val fetch = await(repository.findByUploadId(fileReference).map(_.get))

    fetch.status shouldBe updated.status
    fetch.reference shouldBe updated.reference
    fetch.createDateTime should equal(updated.createDateTime)
  }

  "update a fileStatus to failed" in {
    val fileReference = Reference(UUID.randomUUID().toString)
    val fileStatus    = UploadInfo(fileReference, InProgress, instant)
    await(repository.requestUpload(fileStatus))

    val retrieved = await(repository.findByUploadId(fileReference))

    retrieved.value.status shouldBe fileStatus.status
    retrieved.value.reference shouldBe fileStatus.reference
    retrieved.value.createDateTime should equal(fileStatus.createDateTime)

    val updated = fileStatus.copy(status = Failed)

    await(repository.updateStatus(reference = fileReference, Failed))
    val fetch = await(repository.findByUploadId(fileReference).map(_.get))

    fetch.status shouldBe updated.status
    fetch.reference shouldBe updated.reference
    fetch.createDateTime should equal(updated.createDateTime)

  }

  "update a fileStatus to InProgress" in {
    val fileReference = Reference(UUID.randomUUID().toString)
    val fileStatus    = UploadInfo(fileReference, InProgress, instant)
    await(repository.requestUpload(fileStatus))

    val retrieved = await(repository.findByUploadId(fileReference))

    retrieved.value.status shouldBe fileStatus.status
    retrieved.value.reference shouldBe fileStatus.reference
    retrieved.value.createDateTime should equal(fileStatus.createDateTime)

    val updated = fileStatus.copy(status = InProgress)

    await(repository.updateStatus(reference = fileReference, InProgress))
    val fetch = await(repository.findByUploadId(fileReference).map(_.get))

    fetch.status shouldBe updated.status
    fetch.reference shouldBe updated.reference
    fetch.createDateTime should equal(updated.createDateTime)

  }

}
