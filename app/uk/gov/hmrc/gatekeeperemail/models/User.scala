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

import scala.collection.JavaConverters.asScalaBufferConverter

import play.api.ConfigLoader
import play.api.libs.json._

trait User {
  def email: String
  def firstName: String
  def lastName: String
}

case class RegisteredUser(
    email: String,
    firstName: String,
    lastName: String,
    verified: Boolean
  ) extends User {}

object RegisteredUser {
  implicit val registeredUserFormat = Json.format[RegisteredUser]

  implicit val configLoader: ConfigLoader[List[RegisteredUser]] = ConfigLoader(_.getConfigList).map(
    _.asScala.toList.map(config =>
      RegisteredUser(
        config.getString("email"),
        config.getString("firstName"),
        config.getString("lastName"),
        if (config.hasPath("verified")) config.getBoolean("verified") else true
      )
    )
  )
}
