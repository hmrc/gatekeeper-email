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

package uk.gov.hmrc.gatekeeperemail.connectors

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.models._
import uk.gov.hmrc.gatekeeperemail.models.TopicOptionChoice.TopicOptionChoice
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import com.google.inject.name.Named
import uk.gov.hmrc.gatekeeperemail.connectors.DeveloperStatusFilter.{AllStatus, DeveloperStatusFilter}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.gatekeeperemail.encryption.{PayloadEncryption, SecretRequest, SendsSecretRequest}


case class ApiContextVersion(context: ApiContext, version: ApiVersion) {
  def toStringValue: String = s"${context.value}__${version.value}"
}
case class UnregisteredUser(
                             override val email: String,
                             override val firstName: String,
                             override val lastName: String,
                             verified: Boolean) extends User
case object DeveloperStatusFilter {

  sealed trait DeveloperStatusFilter {
    def isMatch(user: User): Boolean

    val value: String
  }

  case object VerifiedStatus extends DeveloperStatusFilter {
    val value = "VERIFIED"

    override def isMatch(user: User): Boolean = user match {
      case r : RegisteredUser => r.verified
      case u : UnregisteredUser => true   // TODO - really true ??
    }
  }

  case object UnverifiedStatus extends DeveloperStatusFilter {
    val value = "UNVERIFIED"

    override def isMatch(user: User): Boolean = !VerifiedStatus.isMatch(user)
  }

  case object AllStatus extends DeveloperStatusFilter {
    val value = "ALL"

    override def isMatch(user: User): Boolean = true
  }

  def apply(value: Option[String]): DeveloperStatusFilter = {
    value match {
      case Some(UnverifiedStatus.value) => UnverifiedStatus
      case Some(VerifiedStatus.value) => VerifiedStatus
      case Some(AllStatus.value) => AllStatus
      case None => AllStatus
      case Some(text) => throw new Exception("Invalid developer status filter: " + text)
    }
  }
}
case class Developers2Filter(maybeEmailFilter: Option[String] = None,
                             maybeApiFilter: Option[ApiContextVersion] = None,
                             environmentFilter: ApiSubscriptionInEnvironmentFilter = AnyEnvironment,
                             developerStatusFilter: DeveloperStatusFilter = AllStatus)

trait DeveloperConnector {

  def fetchAll()(implicit hc: HeaderCarrier): Future[List[RegisteredUser]]

  def fetchByEmailPreferences(topic: TopicOptionChoice,
                              maybeApis: Option[Seq[String]] = None,
                              maybeApiCategory: Option[Seq[APICategory]] = None,
                              privateapimatch: Boolean = false)(implicit hc: HeaderCarrier): Future[List[RegisteredUser]]
}



@Singleton
class HttpDeveloperConnector @Inject()(appConfig: AppConfig,
                                       sandboxApplicationConnector: SandboxApplicationConnector,
                                       productionApplicationConnector: ProductionApplicationConnector,
                                       http: HttpClient, @Named("ThirdPartyDeveloper") val payloadEncryption: PayloadEncryption)
    (implicit ec: ExecutionContext)
    extends DeveloperConnector
    with SendsSecretRequest {


  def fetchByEmailPreferences(topic: TopicOptionChoice,
                              maybeApis: Option[Seq[String]] = None,
                              maybeApiCategories: Option[Seq[APICategory]] = None,
                              privateapimatch: Boolean = false)(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = {
    val regimes: Seq[(String,String)] = maybeApiCategories.fold(Seq.empty[(String,String)])(regimes =>
                                            regimes.flatMap(regime => Seq("regime" -> regime.value)))
    val privateapimatchParams = if(privateapimatch) Seq("privateapimatch" -> "true") else Seq.empty
    val queryParams =
      Seq("topic" -> topic.toString) ++ regimes ++
      maybeApis.fold(Seq.empty[(String,String)])(apis => apis.map(("service" -> _))) ++ privateapimatchParams

    http.GET[List[RegisteredUser]](s"${appConfig.developerBaseUrl}/developers/email-preferences", queryParams)
  }

  def fetchAll()(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = {
    http.GET[List[RegisteredUser]](s"${appConfig.developerBaseUrl}/developers/all")
  }

  def searchDevelopers(filter: Developers2Filter)(implicit hc: HeaderCarrier): Future[List[User]] = {

    val unsortedResults: Future[List[User]] = (filter.maybeEmailFilter, filter.maybeApiFilter) match {
      case (emailFilter, None) => searchDevelopers(emailFilter, filter.developerStatusFilter)
      case (maybeEmailFilter, Some(apiFilter)) => {
        for {
          collaboratorEmails <- getCollaboratorsByApplicationEnvironments(filter.environmentFilter, maybeEmailFilter, apiFilter)
          users <- fetchByEmails(collaboratorEmails)
          filteredRegisteredUsers <- Future.successful(users.filter(user => collaboratorEmails.contains(user.email)))
          filteredByDeveloperStatusUsers <- Future.successful(filteredRegisteredUsers.filter(filter.developerStatusFilter.isMatch))
        } yield filteredByDeveloperStatusUsers
      }
    }

    for {
      results <- unsortedResults
    } yield results.sortBy(_.email)
  }

  private def getCollaboratorsByApplicationEnvironments(environmentFilter: ApiSubscriptionInEnvironmentFilter,
                                                        maybeEmailFilter: Option[String],
                                                        apiFilter: ApiContextVersion)
                                                       (implicit hc: HeaderCarrier): Future[Set[String]] = {

    val environmentApplicationConnectors = environmentFilter match {
      case ProductionEnvironment => List(productionApplicationConnector)
      case SandboxEnvironment => List(sandboxApplicationConnector)
      case AnyEnvironment => List(productionApplicationConnector, sandboxApplicationConnector)
    }

    val allCollaboratorEmailsFutures: List[Future[List[String]]] = environmentApplicationConnectors
      .map(_.searchCollaborators(apiFilter.context, apiFilter.version, maybeEmailFilter))

    combine(allCollaboratorEmailsFutures).map(_.toSet)
  }

  private def combine[T](futures: List[Future[List[T]]]): Future[List[T]] = Future.reduceLeft(futures)(_ ++ _)


  def searchDevelopers(maybeEmail: Option[String], status: DeveloperStatusFilter)(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = {

    val payload = SearchParameters(maybeEmail, Some(status.value))

    secretRequest(payload) { request =>
      http.POST[SecretRequest, List[RegisteredUser]](s"${appConfig.developerBaseUrl}/developers/search", request)
    }
  }

  def fetchByEmails(emails: Iterable[String])(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = {
    http.POST[Iterable[String], List[RegisteredUser]](s"${appConfig.developerBaseUrl}/developers/get-by-emails", emails)
  }
}

