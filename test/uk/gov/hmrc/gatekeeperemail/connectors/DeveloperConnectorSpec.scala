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

import scala.concurrent.ExecutionContext.Implicits.global

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo, _}
import com.github.tomakehurst.wiremock.client.WireMock.{verify => wireMockVerify}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.libs.json.Json
import play.api.test.Helpers.OK
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.connectors.DeveloperConnector.RegisteredUser
import uk.gov.hmrc.gatekeeperemail.models._
import uk.gov.hmrc.gatekeeperemail.utils.{AsyncHmrcSpec, _}

class DeveloperConnectorSpec extends AsyncHmrcSpec
    with WireMockSugar
    with BeforeAndAfterEach
    with GuiceOneAppPerSuite
    with UrlEncoding {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val mockAppConfig = mock[AppConfig]
    val httpClient    = app.injector.instanceOf[HttpClient]

    when(mockAppConfig.developerBaseUrl).thenReturn(wireMockUrl)

    val connector = new DeveloperConnector(mockAppConfig, httpClient)
  }
  "Developer connector" should {

    val developerEmail                     = "developer1@example.com"
    val developerEmailWithSpecialCharacter = "developer2+test@example.com"

    def aUserResponse(email: String) = RegisteredUser(email, "first", "last", verified = true)

    def verifyUserResponse(registeredUser: RegisteredUser, expectedEmail: String, expectedFirstName: String, expectedLastName: String) = {
      registeredUser.email shouldBe expectedEmail
      registeredUser.firstName shouldBe expectedFirstName
      registeredUser.lastName shouldBe expectedLastName
    }

    "fetch verified developers" in new Setup {
      stubFor(get(urlEqualTo("/developers/all?status=VERIFIED")).willReturn(
        aResponse().withStatus(OK).withBody(
          Json.toJson(Seq(aUserResponse(developerEmail), aUserResponse(developerEmailWithSpecialCharacter))).toString()
        )
      ))

      val result = await(connector.fetchVerified())

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
        val url      = s"""/developers/email-preferences\\?topic=${TopicOptionChoice.BUSINESS_AND_POLICY.toString}&regime=VAT&regime=OTHER"""
        val user     = aUserResponse(developerEmail)
        val matching = urlMatching(url)

        stubFor(
          get(matching)
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(Json.toJson(Seq(user)).toString())
            )
        )

        val result =
          await(connector.fetchByEmailPreferences(TopicOptionChoice.BUSINESS_AND_POLICY, maybeApis = None, maybeApiCategories = Some(Seq(ApiCategory.VAT, ApiCategory.OTHER))))

        wireMockVerify(getRequestedFor(matching))

        result shouldBe List(user)

      }

      "make a call with topic, api categories and apis passed into the service and return users from response" in new Setup {
        val url      = s"""/developers/email-preferences\\?topic=${TopicOptionChoice.BUSINESS_AND_POLICY.toString}&regime=VAT&regime=OTHER&service=service1&service=service2"""
        val user     = aUserResponse(developerEmail)
        val matching = urlMatching(url)

        stubFor(
          get(matching)
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(Json.toJson(Seq(user)).toString())
            )
        )

        val result = await(connector.fetchByEmailPreferences(
          TopicOptionChoice.BUSINESS_AND_POLICY,
          maybeApis = Some(Seq("service1", "service2")),
          maybeApiCategories = Some(Seq(ApiCategory.VAT, ApiCategory.OTHER))
        ))

        wireMockVerify(getRequestedFor(matching))

        result shouldBe List(user)
      }

      "make a call with topic, api categories and apis passed and privateapimatch as true into the service and return users from response" in new Setup {
        val url      =
          s"""/developers/email-preferences\\?topic=${TopicOptionChoice.BUSINESS_AND_POLICY.toString}&regime=VAT&regime=OTHER&service=service1&service=service2&privateapimatch=true"""
        val user     = aUserResponse(developerEmail)
        val matching = urlMatching(url)

        stubFor(
          get(matching)
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(Json.toJson(Seq(user)).toString())
            )
        )

        val result = await(connector.fetchByEmailPreferences(
          TopicOptionChoice.BUSINESS_AND_POLICY,
          maybeApis = Some(Seq("service1", "service2")),
          maybeApiCategories = Some(Seq(ApiCategory.VAT, ApiCategory.OTHER)),
          privateapimatch = true
        ))

        wireMockVerify(getRequestedFor(matching))

        result shouldBe List(user)
      }
    }
  }

}
