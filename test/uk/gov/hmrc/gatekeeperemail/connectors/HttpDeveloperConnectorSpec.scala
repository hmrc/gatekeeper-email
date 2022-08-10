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

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.WireMock.{verify => wireMockVerify}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import play.api.libs.json.Json
import play.api.test.Helpers.OK
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.WireMock.{verify => wireMockVerify}
import org.mockito.MockitoSugar.mock
import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.models._
import uk.gov.hmrc.gatekeeperemail.utils._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.Json
import play.api.test.Helpers.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.gatekeeperemail.utils.AsyncHmrcSpec
class HttpDeveloperConnectorSpec  extends AsyncHmrcSpec
  with WireMockSugar
  with BeforeAndAfterEach
  with GuiceOneAppPerSuite
  with UrlEncoding {

  trait Setup {
    implicit val hc = HeaderCarrier()

    val mockAppConfig = mock[AppConfig]
    val httpClient = app.injector.instanceOf[HttpClient]

    when(mockAppConfig.developerBaseUrl).thenReturn(wireMockUrl)

    val connector = new HttpDeveloperConnector(mockAppConfig, httpClient)
  }
  "Developer connector" should {

    val developerEmail = "developer1@example.com"
    val developerEmailWithSpecialCharacter = "developer2+test@example.com"

    def aUserResponse(email: String) = RegisteredUser(email, "first", "last", verified = false)

    def verifyUserResponse(userResponse: User,
                           expectedEmail: String,
                           expectedFirstName: String, expectedLastName: String) = {
      userResponse.email shouldBe expectedEmail
      userResponse.firstName shouldBe expectedFirstName
      userResponse.lastName shouldBe expectedLastName
    }
    
    "fetch all developers" in new Setup {
      stubFor(get(urlEqualTo("/developers/all")).willReturn(
        aResponse().withStatus(OK).withBody(
          Json.toJson(Seq(aUserResponse(developerEmail), aUserResponse(developerEmailWithSpecialCharacter))).toString()))
      )

      val result = await(connector.fetchAll())

      verifyUserResponse(result(0), developerEmail, "first", "last")
      verifyUserResponse(result(1), developerEmailWithSpecialCharacter, "first", "last")
    }

    "Search by Email Preferences" should {
      val url = s"/developers/email-preferences"

      "make a call with topic passed into the service and return users from response" in new Setup {
        val user = aUserResponse(developerEmail)

        stubFor(
          get(urlPathEqualTo(url))
            .withQueryParam("topic", equalTo(TopicOptionChoice.BUSINESS_AND_POLICY.toString))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(Json.toJson(Seq(user)).toString())
            )
        )

        val result = await(connector.fetchByEmailPreferences(TopicOptionChoice.BUSINESS_AND_POLICY))

        wireMockVerify(getRequestedFor(urlPathEqualTo(url)))

        result shouldBe List(user)
      }

      "make a call with topic and api category passed into the service and return users from response" in new Setup {
        val url = s"""/developers/email-preferences\\?topic=${TopicOptionChoice.BUSINESS_AND_POLICY.toString}&regime=VAT&regime=API1"""
        val user = aUserResponse(developerEmail)
        val matching = urlMatching(url)

        stubFor(
          get(matching)
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(Json.toJson(Seq(user)).toString())
            )
        )

        val result = await(connector.fetchByEmailPreferences(TopicOptionChoice.BUSINESS_AND_POLICY, maybeApis = None, maybeApiCategories = Some(Seq(APICategory("VAT"), APICategory("API1")))))

        wireMockVerify(getRequestedFor(matching))

        result shouldBe List(user)

      }

      "make a call with topic, api categories and apis passed into the service and return users from response" in new Setup {
        val url = s"""/developers/email-preferences\\?topic=${TopicOptionChoice.BUSINESS_AND_POLICY.toString}&regime=VAT&regime=API1&service=service1&service=service2"""
        val user = aUserResponse(developerEmail)
        val matching = urlMatching(url)

        stubFor(
          get(matching)
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(Json.toJson(Seq(user)).toString())
            )
        )

        val result = await(connector.fetchByEmailPreferences(TopicOptionChoice.BUSINESS_AND_POLICY, maybeApis = Some(Seq("service1", "service2")), maybeApiCategories = Some(Seq(APICategory("VAT"), APICategory("API1")))))

        wireMockVerify(getRequestedFor(matching))

        result shouldBe List(user)
      }

      "make a call with topic, api categories and apis passed and privateapimatch as true into the service and return users from response" in new Setup {
        val url = s"""/developers/email-preferences\\?topic=${TopicOptionChoice.BUSINESS_AND_POLICY.toString}&regime=VAT&regime=API1&service=service1&service=service2&privateapimatch=true"""
        val user = aUserResponse(developerEmail)
        val matching = urlMatching(url)

        stubFor(
          get(matching)
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(Json.toJson(Seq(user)).toString())
            )
        )

        val result = await(connector.fetchByEmailPreferences(TopicOptionChoice.BUSINESS_AND_POLICY, maybeApis = Some(Seq("service1", "service2")), maybeApiCategories = Some(Seq(APICategory("VAT"), APICategory("API1"))), privateapimatch = true))

        wireMockVerify(getRequestedFor(matching))

        result shouldBe List(user)
      }
    }
  }



}
