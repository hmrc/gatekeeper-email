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

package uk.gov.hmrc.gatekeeperemail.connectors

import java.io.IOException
import scala.concurrent.ExecutionContext.Implicits.global

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{verify => wireMockVerify, _}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.http.Fault
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.http.Status.OK
import play.mvc.Http.Status
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.HttpClientV2Support

import uk.gov.hmrc.gatekeeperemail.common.AsyncHmrcTestSpec
import uk.gov.hmrc.gatekeeperemail.config.EmailRendererConnectorConfig
import uk.gov.hmrc.gatekeeperemail.connectors.DeveloperConnector.RegisteredUser
import uk.gov.hmrc.gatekeeperemail.models.requests.{DevelopersEmailQuery, DraftEmailRequest}

class GatekeeperEmailRendererConnectorSpec extends AsyncHmrcTestSpec with BeforeAndAfterEach with BeforeAndAfterAll with GuiceOneAppPerSuite with HttpClientV2Support {

  val stubPort       = sys.env.getOrElse("WIREMOCK", "22222").toInt
  val stubHost       = "localhost"
  val wireMockUrl    = s"http://$stubHost:$stubPort"
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterEach(): Unit = {
    wireMockServer.resetMappings()
    super.afterEach()
  }

  override def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

  val gatekeeperLink    = "http://some.url"
  val emailId           = "email@example.com"
  val subject           = "Email subject"
  val fromAddress       = "gateKeeper"
  val emailBody         = "Body to be used in the email template"
  val templateId        = "gatekeeper"
  val emailRendererPath = s"/templates/$templateId"
  val users             = List(RegisteredUser("example@example.com", "first name", "last name", true), RegisteredUser("example2@example2.com", "first name2", "last name2", true))
  val emailPreferences  = DevelopersEmailQuery()

  trait Setup {

    val fakeEmailRendererConnectorConfig = new EmailRendererConnectorConfig {
      val emailRendererBaseUrl = wireMockUrl
    }

    implicit val hc: HeaderCarrier = HeaderCarrier()

    lazy val underTest = new GatekeeperEmailRendererConnector(httpClientV2, fakeEmailRendererConnectorConfig)
  }

  trait WorkingHttp {
    self: Setup =>
    stubFor(post(urlEqualTo(emailRendererPath)).willReturn(aResponse().withBody(
      s"""{"plain": "RGVhciB1c2VyLCBUaGlzIGlzIGEgdGVzdCBtYWls",
         |"html": "PGgyPkRlYXIgdXNlcjwvaDI+LCA8YnI+VGhpcyBpcyBhIHRlc3QgbWFpbA==", "fromAddress": "fromAddress",
         |"service": "service", "subject": "subject"}""".stripMargin
    ).withStatus(OK)))
  }

  trait FailingHttp {
    self: Setup =>
    stubFor(post(urlEqualTo(emailRendererPath)).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)))
  }

  trait NotFoundHttp {
    self: Setup =>
    stubFor(post(urlEqualTo(emailRendererPath)).willReturn(aResponse().withStatus(Status.NOT_FOUND)))
  }

  "emailRendererConnector" should {
    val parameters: Map[String, String] = Map("subject" -> s"$subject", "fromAddress" -> s"$fromAddress", "body" -> s"$emailBody", "service" -> s"gatekeeper")
    val emailRequest                    = DraftEmailRequest(emailPreferences, templateId, parameters)

    "get gatekeeper email template renderer" in new Setup with WorkingHttp {
      await(underTest.getTemplatedEmail(emailRequest))

      wireMockVerify(
        1,
        postRequestedFor(
          urlEqualTo(emailRendererPath)
        )
          .withRequestBody(equalToJson(
            s"""
               |{
               |  "parameters": {
               |    "subject": "$subject",
               |    "fromAddress": "gateKeeper",
               |    "body": "$emailBody",
               |    "service": "gatekeeper"
               |  }
               |}""".stripMargin
          ))
      )
    }

    "fail to get gatekeeper email template renderer" in new Setup with FailingHttp {
      intercept[IOException] {
        await(underTest.getTemplatedEmail(emailRequest))
      }
    }

    "fail to get email template as the renderer returns 404" in new Setup with NotFoundHttp {
      val result = await(underTest.getTemplatedEmail(emailRequest))

      result match {
        case Left(ex: Exception) =>
          ex.getMessage shouldBe "POST of 'http://localhost:22222/templates/gatekeeper' returned 404. Response body: ''"
          ex.statusCode shouldBe Status.NOT_FOUND
        case Right(_)            => fail()
      }
    }
  }
}
