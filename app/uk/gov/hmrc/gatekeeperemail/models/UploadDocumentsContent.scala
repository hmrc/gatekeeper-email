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

package uk.gov.hmrc.gatekeeperemail.models

import play.api.libs.json.{Json, OFormat}

case class UploadDocumentsContent(
                                   serviceName: Option[String] = None,
                                   title: Option[String] = None,
                                   descriptionHtml: Option[String] = None,
                                   serviceUrl: Option[String] = None,
                                   accessibilityStatementUrl: Option[String] = None,
                                   phaseBanner: Option[String] = None,
                                   phaseBannerUrl: Option[String] = None,
                                   userResearchBannerUrl: Option[String] = None,
                                   signOutUrl: Option[String] = None,
                                   timedOutUrl: Option[String] = None,
                                   keepAliveUrl: Option[String] = None,
                                   timeoutSeconds: Option[Int] = None,
                                   countdownSeconds: Option[Int] = None,
                                   showLanguageSelection: Option[Boolean] = None,
                                   pageTitleClasses: Option[String] = None,
                                   allowedFilesTypesHint: Option[String] = None,
                                   contactFrontendServiceId: Option[String] = None
                                 )

object UploadDocumentsContent {
  implicit val format: OFormat[UploadDocumentsContent] = Json.format[UploadDocumentsContent]
}
