/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeperemail.config

import play.api.mvc.{EssentialAction, EssentialFilter}
import play.api.routing.Router.Attrs
import play.filters.csrf.CSRFFilter

class FilteringCSRFFilter(filter: CSRFFilter) extends EssentialFilter {

  override def apply(nextFilter: EssentialAction) = new EssentialAction {

    import play.api.mvc._

    override def apply(rh: RequestHeader) = {
      val chainedFilter = filter.apply(nextFilter)

      if (rh.attrs.get(Attrs.HandlerDef).exists(_.comments.contains("NOCSRF"))) {
        nextFilter(rh)
      } else {
        chainedFilter(rh)
      }
    }
  }
}
