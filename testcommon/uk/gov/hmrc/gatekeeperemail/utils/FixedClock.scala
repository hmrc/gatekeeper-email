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

package uk.gov.hmrc.gatekeeperemail.utils

import java.time.{Clock, LocalDateTime, ZoneOffset}

trait FixedClock {

  val utc = ZoneOffset.UTC

  val now = LocalDateTime.of(2020, 1, 2, 3, 4, 5, 6_000_000).toInstant(utc)

  val clock: Clock = Clock.fixed(now, utc)

  val nowAsText: String = "2020-01-02T03:04:05.006Z"
}
