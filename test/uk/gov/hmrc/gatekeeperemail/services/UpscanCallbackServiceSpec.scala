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

import akka.util.Timeout
import org.mockito.MockitoSugar.{mock, when}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, Matchers}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.await
import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.controllers.{CallbackBody, ErrorDetails, FailedCallbackBody, ReadyCallbackBody, UploadDetails}
import uk.gov.hmrc.gatekeeperemail.models.{Failed, Reference, UploadId, UploadedFailedWithErrors, UploadedSuccessfully}
import uk.gov.hmrc.gatekeeperemail.repositories.{FileUploadStatusRepository, UploadInfo}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport
import uk.gov.hmrc.objectstore.client.{Md5Hash, ObjectSummaryWithMd5, Path, RetentionPeriod}
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient

import java.time.Instant
import java.util.UUID.randomUUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful
import scala.concurrent.duration.{FiniteDuration, SECONDS}

class UpscanCallbackServiceSpec extends AnyWordSpec with PlayMongoRepositorySupport[UploadInfo] with
  Matchers with BeforeAndAfterEach with GuiceOneAppPerSuite {

  case class DummyCallBackBody(reference: String) extends CallbackBody

  val uploadId = UploadId(randomUUID)
  val reference = randomUUID.toString
  val readyCallbackBody = ReadyCallbackBody(
    reference = reference,
    downloadUrl = "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
    uploadDetails = UploadDetails(
      uploadTimestamp = Instant.parse("2018-04-24T09:30:00Z"),
      checksum = "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
      fileMimeType = "application/pdf",
      fileName = "test.pdf",
      size = 45678L
    ))
  val uploadStatusSuccess = UploadedSuccessfully("test.pdf", "application/pdf", "https://bucketName.s3.eu-west-2.amazonaws.com?1235676", Some(45678L))
  val uploadInfoSuccess = UploadInfo(uploadId, Reference(reference), uploadStatusSuccess)
  val uploadStatusSFailedWithErrors = UploadedFailedWithErrors("FAILED", "QUARANTINE", "This file has a virus", reference)

  val failedCallbackBody = FailedCallbackBody(
    reference = reference,
    fileStatus = "FAILED",
    failureDetails = ErrorDetails(
      failureReason = "QUARANTINE",
      message = "This file has a virus"
    )
  )
  val dummyCallBackBody = DummyCallBackBody(reference)
  val uploadInfoFailed = UploadInfo(uploadId, Reference(reference), uploadStatusSFailedWithErrors)
  implicit val timeout = Timeout(FiniteDuration(20, SECONDS))

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()

  override implicit lazy val app: Application = appBuilder.build()
  override protected def repository = app.injector.instanceOf[FileUploadStatusRepository]
  val objectStoreClient = mock[PlayObjectStoreClient]
  val mockAppConfig = mock[AppConfig]
  when(mockAppConfig.defaultRetentionPeriod).thenReturn("1-year")
  val t = new UpscanCallbackService(repository, objectStoreClient, mockAppConfig)
  val f = new FileUploadStatusService(repository)

  override def beforeEach(): Unit = {
    prepareDatabase()
  }

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val toLocation = Path.File(Path.Directory("gatekeeper-email"), readyCallbackBody.uploadDetails.fileName)
    when(objectStoreClient.uploadFromUrl(from = new URL(readyCallbackBody.downloadUrl), to = toLocation,
      retentionPeriod = RetentionPeriod.OneDay,
      owner = "gatekeeper"
    )
    ).thenReturn(successful(ObjectSummaryWithMd5(toLocation, readyCallbackBody.uploadDetails.size,
      Md5Hash(readyCallbackBody.uploadDetails.checksum), readyCallbackBody.uploadDetails.uploadTimestamp)))
  }
  "UpscanCallbackService" should {

    "handle successful file upload callbackbody" in new Setup {
      await(f.requestUpload(reference))
      await(t.handleCallback(readyCallbackBody)).status shouldBe uploadInfoSuccess.status
      await(t.handleCallback(readyCallbackBody)).reference shouldBe uploadInfoSuccess.reference
    }

    "handle failed file upload callbackbody" in new Setup {
      await(t.handleCallback(failedCallbackBody)).status shouldBe uploadInfoFailed.status
      await(t.handleCallback(failedCallbackBody)).reference shouldBe uploadInfoSuccess.reference
    }

    "handle unknown file upload callbackbody" in new Setup {
      await(t.handleCallback(dummyCallBackBody)).status shouldBe Failed
    }
  }
}
