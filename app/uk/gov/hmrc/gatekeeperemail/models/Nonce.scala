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

import play.api.libs.json.Format

import scala.util.Random

case class Nonce(value: Int) {
  override def equals(o: Any): Boolean =
    o match {
      case nonce: Nonce => nonce.value == value
      case _ => false
    }
}

object Nonce {
  final def random: Nonce = Nonce(Random.nextInt())
  implicit final val formats: Format[Nonce] = SimpleDecimalFormat[Nonce](s => Nonce(s.toIntExact), n => BigDecimal(n.value))
}
