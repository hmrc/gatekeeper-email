package uk.gov.hmrc.gatekeeperemail.repository

import akka.stream.Materializer
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import uk.gov.hmrc.gatekeeperemail.common.AsyncHmrcSpec
import uk.gov.hmrc.gatekeeperemail.models._

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import akka.util.Timeout
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.gatekeeperemail.repositories.{FileUploadStatusRepository, UploadInfo}

import scala.concurrent.duration.{FiniteDuration, SECONDS}
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport

import java.util.UUID
import java.util.UUID.randomUUID
class FileUploadStatusRepositorySpec
  extends AsyncHmrcSpec with BeforeAndAfterEach with BeforeAndAfterAll
    with PlayMongoRepositorySupport[UploadInfo] with
    Matchers with GuiceOneAppPerSuite
{

  implicit var s : ActorSystem = ActorSystem("test")
  implicit var m : Materializer = Materializer(s)
  implicit val timeOut = Timeout(FiniteDuration(20, SECONDS))


  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()

  override implicit lazy val app: Application = appBuilder.build()

  def repository = app.injector.instanceOf[FileUploadStatusRepository]

  override def beforeEach() {
    prepareDatabase()
  }

  override protected def afterAll() {
    prepareDatabase()
  }

  "save" should {
    "create a file upload status and retrieve it from database" in {
      val uploadId = UploadId(randomUUID)
      val fileReference = Reference(UUID.randomUUID().toString)
      val fileStatus = UploadInfo(fileReference, InProgress)
      await(repository.requestUpload(fileStatus))

      val retrieved  = await(repository.findByUploadId(fileReference)).get

      retrieved shouldBe fileStatus

    }
  }

  "update a fileStatus to success" in {
    val fileReference = Reference(UUID.randomUUID().toString)
    val fileStatus = UploadInfo(fileReference, InProgress)
    await(repository.requestUpload(fileStatus))

    val retrieved  = await(repository.findByUploadId(fileReference)).get

    retrieved shouldBe fileStatus

    val updated = fileStatus.copy(status = UploadedSuccessfully("abc.jpeg", "jpeg", "http://s3/abc.jpeg", Some(234), "http://aws.object-url"))
    await(repository.updateStatus(reference = fileReference, UploadedSuccessfully("abc.jpeg", "jpeg", "http://s3/abc.jpeg", Some(234), "http://aws.object-url")))

    val fetch = await(repository.findByUploadId(fileReference).map(_.get))
    fetch shouldBe updated
  }

  "update a fileStatus to failedwithErrors" in {
    val fileReference = Reference(UUID.randomUUID().toString)
    val fileStatus = UploadInfo(fileReference, InProgress)
    await(repository.requestUpload(fileStatus))

    val retrieved  = await(repository.findByUploadId(fileReference)).get

    retrieved shouldBe fileStatus

    val updated = fileStatus.copy(status = UploadedFailedWithErrors("VIRUS", "found Virus", "1233", fileReference.value))

    await(repository.updateStatus(reference = fileReference, UploadedFailedWithErrors("VIRUS", "found Virus", "1233", fileReference.value)))
    val fetch = await(repository.findByUploadId(fileReference).map(_.get))

    fetch shouldBe updated
  }

  "update a fileStatus to failed" in {
    val fileReference = Reference(UUID.randomUUID().toString)
    val fileStatus = UploadInfo(fileReference, InProgress)
    await(repository.requestUpload(fileStatus))

    val retrieved  = await(repository.findByUploadId(fileReference)).get

    retrieved shouldBe fileStatus

    val updated = fileStatus.copy(status = Failed)

    await(repository.updateStatus(reference = fileReference, Failed))
    val fetch = await(repository.findByUploadId(fileReference).map(_.get))

    fetch shouldBe updated
  }

  "update a fileStatus to InProgress" in {
    val uploadId = UploadId(randomUUID)
    val fileReference = Reference(UUID.randomUUID().toString)
    val fileStatus = UploadInfo(fileReference, InProgress)
    await(repository.requestUpload(fileStatus))

    val retrieved  = await(repository.findByUploadId(fileReference)).get

    retrieved shouldBe fileStatus

    val updated = fileStatus.copy(status = InProgress)

    await(repository.updateStatus(reference = fileReference, InProgress))
    val fetch = await(repository.findByUploadId(fileReference).map(_.get))

    fetch shouldBe updated
  }

}
