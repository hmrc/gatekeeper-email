package uk.gov.hmrc.gatekeeperemail.models

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import uk.gov.hmrc.gatekeeperemail.connectors.APIAccessType
import uk.gov.hmrc.gatekeeperemail.models.APICategory


import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import play.api.libs.json.{Format, Json}


sealed trait ApiType extends EnumEntry

object ApiType extends Enum[ApiType] with PlayJsonEnum[ApiType] {
  val values = findValues
  case object REST_API extends ApiType
  case object XML_API extends ApiType
}

case class CombinedApiCategory(value: String) extends AnyVal

object CombinedApiCategory {
  implicit val categoryFormat: Format[CombinedApiCategory] = Json.format[CombinedApiCategory]
  def toAPICategory(combinedApiCategory: CombinedApiCategory) : APICategory = {
    APICategory(combinedApiCategory.value)
  }
}

//TODO -change accessType from being an option when APM version which starts returning this data
// is deployed to production
case class CombinedApi(displayName: String,
                       serviceName: String,
                       categories: List[CombinedApiCategory],
                       apiType: ApiType,
                       accessType: Option[APIAccessType])

object CombinedApi {
  implicit val formatCombinedApi = Json.format[CombinedApi]
}



