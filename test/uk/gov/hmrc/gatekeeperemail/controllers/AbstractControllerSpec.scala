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

package uk.gov.hmrc.gatekeeperemail.controllers

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.{AsyncHmrcSpec, WithCSRFAddToken}
import uk.gov.hmrc.apiplatform.modules.stride.connectors.mocks.AuthConnectorMockModule
import uk.gov.hmrc.gatekeeperemail.stride.config.StrideAuthConfig

class AbstractControllerSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite with WithCSRFAddToken {

  override def fakeApplication() =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"     -> false,
        "metrics.enabled" -> false
      )
      .build()

  trait AbstractSetup extends AuthConnectorMockModule {
    val strideAuthConfig               = app.injector.instanceOf[StrideAuthConfig]
    val fakeRequest                    = FakeRequest().withCSRFToken
    val fakeSubmitCheckedRequest       = fakeRequest.withFormUrlEncodedBody("submit-action" -> "checked")
    val fakeSubmitComebackLaterRequest = fakeRequest.withFormUrlEncodedBody("submit-action" -> "come-back-later")
    val brokenRequest                  = fakeRequest.withFormUrlEncodedBody("submit-action" -> "bobbins")
  }
}
