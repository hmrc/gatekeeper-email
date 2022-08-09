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

package uk.gov.hmrc.gatekeeperemail.services

import java.time.LocalDateTime
import java.util
import java.util.UUID
import com.mongodb.client.result.{InsertManyResult, InsertOneResult}
import org.mockito.{ArgumentCaptor, ArgumentMatchersSugar, MockitoSugar}
import org.mongodb.scala.bson.{BsonInt32, BsonNumber, BsonValue}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.gatekeeperemail.connectors.{ApmConnector, DeveloperConnector, GatekeeperEmailRendererConnector}
import uk.gov.hmrc.gatekeeperemail.models.APIAccessType.{PRIVATE, PUBLIC}
import uk.gov.hmrc.gatekeeperemail.models.EmailStatus._
import uk.gov.hmrc.gatekeeperemail.models.TopicOptionChoice.BUSINESS_AND_POLICY
import uk.gov.hmrc.gatekeeperemail.models._
import uk.gov.hmrc.gatekeeperemail.repositories.{DraftEmailRepository, SentEmailRepository}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful

class DraftEmailServiceSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar with ArgumentMatchersSugar {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val draftEmailRepositoryMock: DraftEmailRepository = mock[DraftEmailRepository]
    val sentEmailRepositoryMock: SentEmailRepository = mock[SentEmailRepository]
    val developerConnectorMock: DeveloperConnector = mock[DeveloperConnector]
    val apmConnectorMock: ApmConnector = mock[ApmConnector]
    val emailRendererConnectorMock: GatekeeperEmailRendererConnector = mock[GatekeeperEmailRendererConnector]
    val underTest = new DraftEmailService(emailRendererConnectorMock, developerConnectorMock, apmConnectorMock,
                                            draftEmailRepositoryMock,   sentEmailRepositoryMock)
    val templateData = EmailTemplateData("templateId", Map(), false, Map(), None)
    val users = List(RegisteredUser("example@example.com", "first name", "last name", true),
      RegisteredUser("example2@example2.com", "first name2", "last name2", true))
    val emailPreferences = DevelopersEmailQuery(allUsers = true)
    val uuid = UUID.randomUUID()
    val now = LocalDateTime.now()
    val email = DraftEmail(uuid.toString(), templateData, "DL Team",
      emailPreferences, None, "markdownEmailBody", "This is test email",
      "test subject", SENT, "composedBy", Some("approvedBy"), now, 2)
    when(emailRendererConnectorMock.getTemplatedEmail(*))
      .thenReturn(successful(Right(RenderResult("RGVhciB1c2VyLCBUaGlzIGlzIGEgdGVzdCBtYWls",
        "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA==", "from@digital.hmrc.gov.uk", "subject", ""))))
  }

  "persistEmail" should {
    "save the email data when sending email to all users into mongodb repo" in new Setup {
      when(draftEmailRepositoryMock.persist(*)).thenReturn(Future(InsertOneResult.acknowledged(BsonNumber(1))))
      when(developerConnectorMock.fetchAll()(*)).thenReturn(Future(users))


      val emailRequest = EmailRequest(emailPreferences, "gatekeeper",
        EmailData("Test subject", "Dear Mr XYZ, This is test email"), false, Map())
      val emailFromMongo: DraftEmail = await(underTest.persistEmail(emailRequest, "emailUUID"))
      emailFromMongo.subject shouldBe "Test subject"
      emailFromMongo.htmlEmailBody shouldBe "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA=="
      emailFromMongo.templateData.templateId shouldBe "gatekeeper"
    }

    "save the email data when sending email to overridden emails addresses into mongodb repo" in new Setup {
      val overriddenPref = DevelopersEmailQuery(emailsForSomeCases = Some(EmailOverride(users, true)))
      when(draftEmailRepositoryMock.persist(*)).thenReturn(Future(InsertOneResult.acknowledged(BsonNumber(1))))
      when(draftEmailRepositoryMock.getEmailData(*)).thenReturn(Future(email.copy(userSelectionQuery = overriddenPref)))
      when(developerConnectorMock.fetchAll()(*)).thenReturn(Future(users))


      val emailRequest = EmailRequest(overriddenPref, "gatekeeper",
        EmailData("Test subject", "Dear Mr XYZ, This is test email"), false, Map())
      val emailFromMongo: DraftEmail = await(underTest.persistEmail(emailRequest, "emailUUID"))
      emailFromMongo.subject shouldBe "Test subject"
      emailFromMongo.htmlEmailBody shouldBe "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA=="
      emailFromMongo.templateData.templateId shouldBe "gatekeeper"
    }

    "save the email data when sending email to subscriptions emails addresses into mongodb repo" in new Setup {
      when(draftEmailRepositoryMock.persist(*)).thenReturn(Future(InsertOneResult.acknowledged(BsonNumber(1))))
      when(developerConnectorMock.fetchAll()(*)).thenReturn(Future(users))

      val overriddenPref = DevelopersEmailQuery(emailsForSomeCases = Some(EmailOverride(users, false)))

      val emailRequest = EmailRequest(overriddenPref, "gatekeeper",
        EmailData("Test subject", "Dear Mr XYZ, This is test email"), false, Map())
      val emailFromMongo: DraftEmail = await(underTest.persistEmail(emailRequest, "emailUUID"))
      emailFromMongo.subject shouldBe "Test subject"
      emailFromMongo.htmlEmailBody shouldBe "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA=="
      emailFromMongo.templateData.templateId shouldBe "gatekeeper"
    }

    "save the email data when sending email to a specific topic emails addresses into mongodb repo" in new Setup {
      when(draftEmailRepositoryMock.persist(*)).thenReturn(Future(InsertOneResult.acknowledged(BsonNumber(1))))
      when(developerConnectorMock.fetchByEmailPreferences(*, *, *, *)(*)).thenReturn(Future(users))

      val overriddenPref = DevelopersEmailQuery(topic = Some("TECHNICAL"))

      val emailRequest = EmailRequest(overriddenPref, "gatekeeper",
        EmailData("Test subject", "Dear Mr XYZ, This is test email"), false, Map())
      val emailFromMongo: DraftEmail = await(underTest.persistEmail(emailRequest, "emailUUID"))
      emailFromMongo.subject shouldBe "Test subject"
      emailFromMongo.htmlEmailBody shouldBe "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA=="
      emailFromMongo.templateData.templateId shouldBe "gatekeeper"
    }

    "save the email data when sending email to a specific API emails addresses into mongodb repo" in new Setup {
      when(draftEmailRepositoryMock.persist(*)).thenReturn(Future(InsertOneResult.acknowledged(BsonNumber(1))))
      when(developerConnectorMock.fetchByEmailPreferences(*, *, *, *)(*)).thenReturn(Future(users))
      when(apmConnectorMock.fetchAllCombinedApis()(*)).thenReturn(Future(List(
        CombinedApi("VAT", "VAT", List(CombinedApiCategory("TAX")), ApiType.REST_API, Some(PUBLIC)),
        CombinedApi("CORP", "VAT", List(CombinedApiCategory("TAX")), ApiType.REST_API, Some(PRIVATE)),
        CombinedApi("SELF", "VAT", List(CombinedApiCategory("TAX")), ApiType.REST_API, Some(PRIVATE))))
      )

      val overriddenPref = DevelopersEmailQuery(topic = Some("TECHNICAL"), apis = Some(Seq("VAT", "CORP")))

      val emailRequest = EmailRequest(overriddenPref, "gatekeeper",
        EmailData("Test subject", "Dear Mr XYZ, This is test email"), false, Map())
      val emailFromMongo: DraftEmail = await(underTest.persistEmail(emailRequest, "emailUUID"))
      emailFromMongo.subject shouldBe "Test subject"
      emailFromMongo.htmlEmailBody shouldBe "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA=="
      emailFromMongo.templateData.templateId shouldBe "gatekeeper"
    }

    "save the email data when sending email to a specific API emails addresses into mongodb repo when privateapi selection is true" in new Setup {
      when(draftEmailRepositoryMock.persist(*)).thenReturn(Future(InsertOneResult.acknowledged(BsonNumber(1))))
      when(developerConnectorMock.fetchByEmailPreferences(TopicOptionChoice.TECHNICAL, Some(List("VAT")),
        Some(List(APICategory("TAX"))), true)(hc)).thenReturn(Future(users))
      when(developerConnectorMock.fetchByEmailPreferences(*, *, *, *)(*)).thenReturn(Future(users))
      when(apmConnectorMock.fetchAllCombinedApis()(*)).thenReturn(Future(List(
        CombinedApi("VAT", "VAT", List(CombinedApiCategory("TAX")), ApiType.REST_API, Some(PUBLIC)),
        CombinedApi("CORP", "VAT", List(CombinedApiCategory("TAX")), ApiType.REST_API, Some(PRIVATE)),
        CombinedApi("SELF", "VAT", List(CombinedApiCategory("TAX")), ApiType.REST_API, Some(PRIVATE))))
      )

      val overriddenPref = DevelopersEmailQuery(topic = Some("TECHNICAL"), apis = Some(Seq("VAT", "CORP")), privateapimatch = true)

      val emailRequest = EmailRequest(overriddenPref, "gatekeeper",
        EmailData("Test subject", "Dear Mr XYZ, This is test email"), false, Map())
      val emailFromMongo: DraftEmail = await(underTest.persistEmail(emailRequest, "emailUUID"))
      emailFromMongo.subject shouldBe "Test subject"
      emailFromMongo.htmlEmailBody shouldBe "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA=="
      emailFromMongo.templateData.templateId shouldBe "gatekeeper"
    }

    "do not save the email data when sending email to a specific API which are empty stringed" in new Setup {
      when(draftEmailRepositoryMock.persist(*)).thenReturn(Future(InsertOneResult.acknowledged(BsonNumber(1))))
      when(developerConnectorMock.fetchByEmailPreferences(TopicOptionChoice.TECHNICAL, Some(List("VAT")),
        Some(List(APICategory("TAX"))), true)(hc)).thenReturn(Future(users))
      when(developerConnectorMock.fetchByEmailPreferences(*, *, *, *)(*)).thenReturn(Future(users))
      when(apmConnectorMock.fetchAllCombinedApis()(*)).thenReturn(Future(List(
        CombinedApi("VAT", "VAT", List(CombinedApiCategory("TAX")), ApiType.REST_API, Some(PUBLIC)),
        CombinedApi("CORP", "VAT", List(CombinedApiCategory("TAX")), ApiType.REST_API, Some(PRIVATE)),
        CombinedApi("SELF", "VAT", List(CombinedApiCategory("TAX")), ApiType.REST_API, Some(PRIVATE))))
      )

      val overriddenPref = DevelopersEmailQuery(topic = Some("TECHNICAL"), apis = Some(Seq("", "")), privateapimatch = true)

      val emailRequest = EmailRequest(overriddenPref, "gatekeeper",
        EmailData("Test subject", "Dear Mr XYZ, This is test email"), false, Map())
      val emailFromMongo: DraftEmail = await(underTest.persistEmail(emailRequest, "emailUUID"))
      emailFromMongo.subject shouldBe "Test subject"
      emailFromMongo.htmlEmailBody shouldBe "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA=="
      emailFromMongo.templateData.templateId shouldBe "gatekeeper"
    }


    "save the email data  with zero recipients when sending email to a specific API  which are not in list of API dictionary into mongodb repo" in new Setup {
      when(draftEmailRepositoryMock.persist(*)).thenReturn(Future(InsertOneResult.acknowledged(BsonNumber(1))))
      when(developerConnectorMock.fetchByEmailPreferences(*, *, *, *)(*)).thenReturn(Future(users))
      when(apmConnectorMock.fetchAllCombinedApis()(*)).thenReturn(Future(List(
        CombinedApi("VAT", "VAT", List(CombinedApiCategory("TAX")), ApiType.REST_API, Some(PUBLIC)),
        CombinedApi("CORP", "VAT", List(CombinedApiCategory("TAX")), ApiType.REST_API, Some(PRIVATE)),
        CombinedApi("SELF", "VAT", List(CombinedApiCategory("TAX")), ApiType.REST_API, Some(PRIVATE))))
      )

      val overriddenPref = DevelopersEmailQuery(topic = Some("TECHNICAL"), apis = Some(Seq("VAT1", "CORP1")))

      val emailRequest = EmailRequest(overriddenPref, "gatekeeper",
        EmailData("Test subject", "Dear Mr XYZ, This is test email"), false, Map())
      val emailFromMongo: DraftEmail = await(underTest.persistEmail(emailRequest, "emailUUID"))
      emailFromMongo.subject shouldBe "Test subject"
      emailFromMongo.htmlEmailBody shouldBe "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA=="
      emailFromMongo.templateData.templateId shouldBe "gatekeeper"
    }


    "save the email data into mongodb repo even when fails to send" in new Setup {
      when(draftEmailRepositoryMock.persist(*)).thenReturn(Future(InsertOneResult.acknowledged(BsonNumber(1))))
      when(developerConnectorMock.fetchAll()(*)).thenReturn(Future(users))
      val emailRequest = EmailRequest(emailPreferences, "gatekeeper",
        EmailData("Test subject2", "Dear Mr XYZ, This is test email"), false, Map())
      val emailFromMongo: DraftEmail = await(underTest.persistEmail(emailRequest, "emailUUID"))
      emailFromMongo.subject shouldBe "Test subject2"
      emailFromMongo.htmlEmailBody shouldBe "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA=="
      emailFromMongo.templateData.templateId shouldBe "gatekeeper"
    }

    "throw exception when unable to reach the email renderer" in new Setup {
      when(emailRendererConnectorMock.getTemplatedEmail(*)).thenReturn(successful(Left(UpstreamErrorResponse("error", Status.NOT_FOUND))))
      when(developerConnectorMock.fetchAll()(*)).thenReturn(Future(users))
      when(draftEmailRepositoryMock.persist(*)).thenReturn(Future(InsertOneResult.acknowledged(BsonNumber(1))))
      val emailRequest = EmailRequest(emailPreferences, "gatekeeper",
        EmailData("Test subject2", "Dear Mr XYZ, This is test email"), false, Map())
      val error: EmailRendererConnectionError = intercept[EmailRendererConnectionError] {
        await(underTest.persistEmail(emailRequest, "emailUUID"))
      }
      error.getMessage shouldBe "Failed to connect to Email renderer service. error"
    }
  }

  "updateEmail" should {
    "update the email data into mongodb repo" in new Setup {
      when(draftEmailRepositoryMock.updateEmail(*)).thenReturn(Future(email))
      val emailRequest = EmailRequest(emailPreferences, "gatekeeper",
        EmailData("Test subject", "Dear Mr XYZ, This is test email"), false, Map())
      val emailFromMongo: DraftEmail = await(underTest.updateEmail(emailRequest, "emailUUID"))
      emailFromMongo.subject shouldBe "Test subject"
      emailFromMongo.htmlEmailBody shouldBe "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA=="
      emailFromMongo.templateData.templateId shouldBe "gatekeeper"
    }

    "update the email data into mongodb repo even when fails to send" in new Setup {
      when(draftEmailRepositoryMock.updateEmail(*)).thenReturn(Future(email))
      val emailRequest = EmailRequest(emailPreferences, "gatekeeper",
        EmailData("Test subject2", "Dear Mr XYZ, This is test email"), false, Map())
      val emailFromMongo: DraftEmail = await(underTest.updateEmail(emailRequest, "emailUUID"))
      emailFromMongo.subject shouldBe "Test subject2"
      emailFromMongo.htmlEmailBody shouldBe "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA=="
      emailFromMongo.templateData.templateId shouldBe "gatekeeper"
    }
  }

  "sendEmail" should {
    "successfully send (into Mongo) an email with two recipients" in new Setup {
     val sentEmailCaptor: ArgumentCaptor[List[SentEmail]] = ArgumentCaptor.forClass(classOf[List[SentEmail]])

      val insertIds = new util.HashMap[Integer, BsonValue]{new Integer(1)-> new BsonInt32(33)}
      when(draftEmailRepositoryMock.getEmailData(*)).thenReturn(Future(email))
      when(draftEmailRepositoryMock.updateEmailSentStatus(email.emailUUID, 2)).thenReturn(Future(email))
      when(sentEmailRepositoryMock.persist(sentEmailCaptor.capture())).thenReturn(Future(InsertManyResult.acknowledged(insertIds)))
      when(developerConnectorMock.fetchAll()(*)).thenReturn(Future(users))

      await(underTest.sendEmail(email.emailUUID))

      verify(draftEmailRepositoryMock).getEmailData(email.emailUUID)
      verify(draftEmailRepositoryMock).updateEmailSentStatus(email.emailUUID, email.emailsCount)
//      verify(sentEmailRepositoryMock).persist(*)
      val listOfSentMailsInserted = sentEmailCaptor.getValue
      listOfSentMailsInserted.size shouldBe 2
      listOfSentMailsInserted(0).recipient shouldBe users(0).email
      listOfSentMailsInserted(0).firstName shouldBe users(0).firstName
      listOfSentMailsInserted(0).lastName shouldBe users(0).lastName
      listOfSentMailsInserted(0).failedCount shouldBe 0
      listOfSentMailsInserted(0).status shouldBe PENDING
      listOfSentMailsInserted(1).recipient shouldBe users(1).email
      listOfSentMailsInserted(1).firstName shouldBe users(1).firstName
      listOfSentMailsInserted(1).lastName shouldBe users(1).lastName
      listOfSentMailsInserted(1).failedCount shouldBe 0
      listOfSentMailsInserted(1).status shouldBe PENDING
    }

    "successfully send (into Mongo) an email with two recipients from overridden email addresses" in new Setup {
      val sentEmailCaptor: ArgumentCaptor[List[SentEmail]] = ArgumentCaptor.forClass(classOf[List[SentEmail]])

      val insertIds = new util.HashMap[Integer, BsonValue]{new Integer(1)-> new BsonInt32(33)}
      when(draftEmailRepositoryMock.getEmailData(*)).thenReturn(Future(
        email.copy(userSelectionQuery = DevelopersEmailQuery(emailsForSomeCases =  Some(EmailOverride(users, true))))))
      when(draftEmailRepositoryMock.updateEmailSentStatus(email.emailUUID, 2)).thenReturn(Future(email))
      when(sentEmailRepositoryMock.persist(sentEmailCaptor.capture())).thenReturn(Future(InsertManyResult.acknowledged(insertIds)))
      when(developerConnectorMock.fetchAll()(*)).thenReturn(Future(users))

      await(underTest.sendEmail(email.emailUUID))

      verify(draftEmailRepositoryMock).getEmailData(email.emailUUID)
      verify(draftEmailRepositoryMock).updateEmailSentStatus(email.emailUUID, email.emailsCount)
//      verify(sentEmailRepositoryMock).persist(*)
      val listOfSentMailsInserted = sentEmailCaptor.getValue
      listOfSentMailsInserted.size shouldBe 2
      listOfSentMailsInserted(0).recipient shouldBe users(0).email
      listOfSentMailsInserted(0).firstName shouldBe users(0).firstName
      listOfSentMailsInserted(0).lastName shouldBe users(0).lastName
      listOfSentMailsInserted(0).failedCount shouldBe 0
      listOfSentMailsInserted(0).status shouldBe PENDING
      listOfSentMailsInserted(1).recipient shouldBe users(1).email
      listOfSentMailsInserted(1).firstName shouldBe users(1).firstName
      listOfSentMailsInserted(1).lastName shouldBe users(1).lastName
      listOfSentMailsInserted(1).failedCount shouldBe 0
      listOfSentMailsInserted(1).status shouldBe PENDING
    }

    "not send (into Mongo) an email with zero recipients" in new Setup {
      val sentEmailCaptor: ArgumentCaptor[List[SentEmail]] = ArgumentCaptor.forClass(classOf[List[SentEmail]])

      val insertIds = new util.HashMap[Integer, BsonValue]{new Integer(1)-> new BsonInt32(33)}
      when(draftEmailRepositoryMock.getEmailData(*)).thenReturn(Future(email))
      when(draftEmailRepositoryMock.updateEmailSentStatus(email.emailUUID, 0)).thenReturn(Future(email))
      when(sentEmailRepositoryMock.persist(sentEmailCaptor.capture())).thenReturn(Future(InsertManyResult.acknowledged(insertIds)))
      when(developerConnectorMock.fetchAll()(*)).thenReturn(Future(List.empty))

      await(underTest.sendEmail(email.emailUUID))

      verify(draftEmailRepositoryMock).getEmailData(email.emailUUID)
      verify(draftEmailRepositoryMock).updateEmailSentStatus(email.emailUUID, 0)

    }
  }
}
