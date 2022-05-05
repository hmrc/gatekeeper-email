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

package uk.gov.hmrc.gatekeeperemail.connectors

import java.io.IOException

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{verify => wireMockVerify, _}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.github.tomakehurst.wiremock.http.Fault
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.OK
import uk.gov.hmrc.gatekeeperemail.common.AsyncHmrcTestSpec
import uk.gov.hmrc.gatekeeperemail.config.EmailConnectorConfig
import uk.gov.hmrc.gatekeeperemail.models.{SendEmailRequest, User}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.ExecutionContext.Implicits.global

class GatekeeperEmailConnectorSpec extends AsyncHmrcTestSpec with BeforeAndAfterEach with BeforeAndAfterAll with GuiceOneAppPerSuite {

  val stubPort = sys.env.getOrElse("WIREMOCK", "22222").toInt
  val stubHost = "localhost"
  val wireMockUrl = s"http://$stubHost:$stubPort"
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  override def beforeAll() {
    super.beforeAll()
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterEach() {
    wireMockServer.resetMappings()
    super.afterEach()
  }

  override def afterAll() {
    wireMockServer.stop()
    super.afterAll()
  }

  val gatekeeperLink = "http://some.url"
  val emailId = "email@example.com"
  val subject = "Email subject"
  val fromAddress = "gateKeeper"
  val emailBody = "Body to be used in the email template"
  val emailServicePath = "/developer/email"
  val users = List(User("example@example.com", "first name", "last name", true),
    User("example2@example2.com", "first name2", "last name2", true))
   
  trait Setup {
    val httpClient = app.injector.instanceOf[HttpClient]
    
    val fakeEmailConnectorConfig = new EmailConnectorConfig {
      val emailBaseUrl = wireMockUrl
    }

    implicit val hc = HeaderCarrier()

    lazy val underTest = new GatekeeperEmailConnector(httpClient, fakeEmailConnectorConfig)
  }

  trait WorkingHttp {
      self: Setup =>
    stubFor(post(urlEqualTo(emailServicePath)).willReturn(aResponse().withStatus(OK)))
  }

  trait FailingHttp {
      self: Setup =>
    stubFor(post(urlEqualTo(emailServicePath)).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)))
  }
  
  "emailConnector" should {
    val parameters: Map[String, String] = Map( "showFooter" -> "true",
      "showHmrcBanner" -> "true", "subject" -> s"$subject", "fromAddress" -> s"$fromAddress",
      "body" -> s"$emailBody", "service" -> s"gatekeeper", "lastName" -> "last name2",
      "firstName" -> "first name2")

    val emailRequest = SendEmailRequest("example2@example2.com", "gatekeeper", parameters)

    "send gatekeeper email" in new Setup with WorkingHttp {
      await(underTest.sendEmail(emailRequest))

      wireMockVerify(1, postRequestedFor(
        urlEqualTo(emailServicePath))
        .withRequestBody(equalToJson(
          s"""
              |{
              |  "to" : [ "example2@example2.com" ],
              |  "templateId": "gatekeeper",
              |  "parameters": {
              |  "showFooter" : "true",
              |  "showHmrcBanner" : "true",
              |  "subject": "$subject",
              |  "fromAddress": "gateKeeper",
              |  "body": "$emailBody",
              |  "service": "gatekeeper",
              |  "lastName" : "last name2",
              |  "firstName" : "first name2"
              |  },
              |  "force": false,
              |  "auditData": {},
              |  "tags" : { }
              |}""".stripMargin))
      )
    }

    "fail to send gatekeeper email" in new Setup with FailingHttp {
      intercept[IOException] {
        await(underTest.sendEmail(emailRequest))
      }
    }
  }
}
