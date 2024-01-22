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

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiAccessType
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}

import uk.gov.hmrc.gatekeeperemail.models.{CombinedApi, _}
import uk.gov.hmrc.gatekeeperemail.utils.{AsyncHmrcSpec, _}

class ApmConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val httpClient = app.injector.instanceOf[HttpClient]

    val mockApmConnectorConfig: ApmConnector.Config = mock[ApmConnector.Config]
    when(mockApmConnectorConfig.serviceBaseUrl).thenReturn(wireMockUrl)

    val underTest = new ApmConnector(httpClient, mockApmConnectorConfig)

    val combinedRestApi1 = CombinedApi("displayName1", "serviceName1", List(ApiCategory.CUSTOMS), ApiType.REST_API, ApiAccessType.PUBLIC)
    val combinedXmlApi2  = CombinedApi("displayName2", "serviceName2", List(ApiCategory.VAT), ApiType.XML_API, ApiAccessType.PUBLIC)
    val combinedList     = List(combinedRestApi1, combinedXmlApi2)

  }

  "fetchAllCombinedApis" should {
    "returns combined xml and rest apis" in new Setup {
      val url = "/combined-rest-xml-apis"

      stubFor(
        get(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(combinedList).toString)
          )
      )

      val result = await(underTest.fetchAllCombinedApis())
      result shouldBe combinedList
    }

    "returns exception when backend returns error" in new Setup {
      val url = "/combined-rest-xml-apis"

      stubFor(
        get(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[UpstreamErrorResponse] {
        await(underTest.fetchAllCombinedApis())
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }

}
