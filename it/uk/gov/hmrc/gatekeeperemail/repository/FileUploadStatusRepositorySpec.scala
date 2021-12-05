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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random.nextString
import akka.actor.ActorSystem
import akka.util.Timeout
import play.api.test.Helpers.await
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.gatekeeperemail.connectors
import uk.gov.hmrc.gatekeeperemail.model.{InProgress, UploadId, UploadedSuccessfully}

import scala.compat.java8.converterImpl.StepperShape.Reference
import scala.concurrent.duration.{FiniteDuration, SECONDS}
import uk.gov.hmrc.gatekeeperemail.connectors.Reference
class FileUploadStatusRepositorySpec
  extends AsyncHmrcSpec with MongoSpecSupport
    with BeforeAndAfterEach with BeforeAndAfterAll
    with IndexVerification
{

  implicit var s : ActorSystem = ActorSystem("test")
  implicit var m : Materializer = Materializer(s)
  implicit val timeOut = Timeout(FiniteDuration(20, SECONDS))
  private val reactiveMongoComponent = new ReactiveMongoComponent {
    override def mongoConnector: MongoConnector = mongoConnectorForTest
  }

  private val fileuploadStatusRepo = new FileUploadStatusRepository(reactiveMongoComponent)

  override def beforeEach() {
    List(fileuploadStatusRepo).foreach { db =>
      await(db.drop)
      await(db.ensureIndexes)
    }
  }

  override protected def afterAll() {
    List(fileuploadStatusRepo).foreach { db =>
      await(db.drop)
      await(s.terminate)
    }
  }

  "save" should {
    "create a file upload status and retrieve it from database" in {
      val uploadId = UploadId("123sdr5423")
      val fileReference = connectors.Reference("232232")
      val fileStatus = UploadInfo(BSONObjectID.generate(), uploadId, fileReference, InProgress)
      await(fileuploadStatusRepo.requestUpload(fileStatus))

      val retrieved  = await(fileuploadStatusRepo.findByUploadId(uploadId)).get

      retrieved shouldBe fileStatus

    }
  }

  "update a fileStatus" in {
    val uploadId = UploadId("123sdr5423")
    val fileReference = connectors.Reference("232232")
    val fileStatus = UploadInfo(BSONObjectID.generate(), uploadId, fileReference, InProgress)
    await(fileuploadStatusRepo.requestUpload(fileStatus))

    val retrieved  = await(fileuploadStatusRepo.findByUploadId(uploadId)).get

    retrieved shouldBe fileStatus

    val updated = fileStatus.copy(status = UploadedSuccessfully("abc.jpeg", "jpeg", "http://s3/abc.jpeg", Some(234)))

    val newRetrieved = await(fileuploadStatusRepo.updateStatus(reference = fileReference, UploadedSuccessfully("abc.jpeg", "jpeg", "http://s3/abc.jpeg", Some(234))))

    val fetch = await(fileuploadStatusRepo.findByUploadId(uploadId).map(_.get))
    fetch shouldBe updated


  }

}
