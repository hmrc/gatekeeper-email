package uk.gov.hmrc.gatekeeperemail.models

import play.api.libs.json.Json

final case class SearchParameters(emailFilter: Option[String], status: Option[String])

object SearchParameters{
  implicit val searchParametersFormat = Json.format[SearchParameters]
}
