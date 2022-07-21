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

package uk.gov.hmrc.gatekeeperemail.models

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import play.api.libs.json.Json

import scala.util.Random
import java.net.URLEncoder.encode

case class ApiContext(value: String) extends AnyVal {
  def urlEncode = encode(value, "UTF-8")
}

object ApiContext {
  implicit val ordering: Ordering[ApiContext] = new Ordering[ApiContext] {
    override def compare(x: ApiContext, y: ApiContext): Int = x.value.compareTo(y.value)
  }
  implicit val formatApiContext = Json.valueFormat[ApiContext]
  def random = ApiContext(Random.alphanumeric.take(10).mkString)
}

case class ApiVersion(value: String) extends AnyVal {
  def urlEncode = encode(value, "UTF-8")
}

object ApiVersion {
  implicit val ordering: Ordering[ApiVersion] = new Ordering[ApiVersion] {
    override def compare(x: ApiVersion, y: ApiVersion): Int = x.value.compareTo(y.value)
  }
  implicit val formatApiVersion = Json.valueFormat[ApiVersion]

  def random = ApiVersion(Random.nextDouble().toString)
}



case class APICategory(value: String) extends AnyVal
object APICategory{
  implicit val formatApiCategory = Json.valueFormat[APICategory]
}

case class APICategoryDetails(category: String, name: String){
  def toAPICategory: APICategory ={
    APICategory(category)
  }
}
object APICategoryDetails{
  implicit val formatApiCategory = Json.format[APICategoryDetails]
}

