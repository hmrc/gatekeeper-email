package uk.gov.hmrc.gatekeeperemail.repository

import akka.stream.Materializer
import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import uk.gov.hmrc.mongo.{MongoConnector, MongoSpecSupport}
import uk.gov.hmrc.gatekeeperemail.common.AsyncHmrcSpec
import uk.gov.hmrc.gatekeeperemail.models.Reference

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random.nextString
import akka.actor.ActorSystem
import akka.util.Timeout
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.await
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.gatekeeperemail.connectors

import scala.concurrent.duration.{FiniteDuration, SECONDS}
import uk.gov.hmrc.gatekeeperemail.models.{InProgress, UploadId, UploadedSuccessfully}
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport

import java.util.UUID
class FileUploadStatusRepositorySpec
  extends AsyncHmrcSpec with BeforeAndAfterEach with BeforeAndAfterAll
    with IndexVerification with PlayMongoRepositorySupport[UploadInfo] with
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
      val uploadId = UploadId.generate
      val fileReference = Reference(UUID.randomUUID().toString)
      val fileStatus = UploadInfo(uploadId, fileReference, InProgress)
      await(repository.requestUpload(fileStatus))

      val retrieved  = await(repository.findByUploadId(uploadId)).get

      retrieved shouldBe fileStatus

    }
  }

  "update a fileStatus" in {
    val uploadId = UploadId.generate
    val fileReference = Reference(UUID.randomUUID().toString)
    val fileStatus = UploadInfo(uploadId, fileReference, InProgress)
    await(repository.requestUpload(fileStatus))

    val retrieved  = await(repository.findByUploadId(uploadId)).get

    retrieved shouldBe fileStatus

    val updated = fileStatus.copy(status = UploadedSuccessfully("abc.jpeg", "jpeg", "http://s3/abc.jpeg", Some(234)))

    val newRetrieved = await(repository.updateStatus(reference = fileReference, UploadedSuccessfully("abc.jpeg", "jpeg", "http://s3/abc.jpeg", Some(234))))

    val fetch = await(repository.findByUploadId(uploadId).map(_.get))
    fetch shouldBe updated
  }

}
