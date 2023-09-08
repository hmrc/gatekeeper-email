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

package uk.gov.hmrc.gatekeeperemail.services

import java.time.Clock
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import uk.gov.hmrc.gatekeeperemail.config.AppConfig
import uk.gov.hmrc.gatekeeperemail.connectors.DeveloperConnector.RegisteredUser
import uk.gov.hmrc.gatekeeperemail.connectors.{ApmConnector, DeveloperConnector, GatekeeperEmailRendererConnector}
import uk.gov.hmrc.gatekeeperemail.models.APIAccessType.{PRIVATE, PUBLIC}
import uk.gov.hmrc.gatekeeperemail.models.CombinedApiCategory.toAPICategory
import uk.gov.hmrc.gatekeeperemail.models._
import uk.gov.hmrc.gatekeeperemail.models.requests.{DevelopersEmailQuery, DraftEmailRequest, EmailOverride, EmailRequest}
import uk.gov.hmrc.gatekeeperemail.repositories.{DraftEmailRepository, SentEmailRepository}
import uk.gov.hmrc.gatekeeperemail.util.ClockNow

@Singleton
class DraftEmailService @Inject() (
    emailRendererConnector: GatekeeperEmailRendererConnector,
    developerConnector: DeveloperConnector,
    apmConnector: ApmConnector,
    draftEmailRepository: DraftEmailRepository,
    sentEmailRepository: SentEmailRepository,
    appConfig: AppConfig,
    override val clock: Clock
  )(implicit val ec: ExecutionContext
  ) extends ClockNow {

  val logger: Logger = Logger(getClass.getName)

  def persistEmail(emailRequest: EmailRequest, emailUUID: String): Future[DraftEmail] = {
    val email: DraftEmail = emailData(emailRequest, emailUUID)

    val sendEmailRequest =
      DraftEmailRequest(emailRequest.userSelectionQuery, emailRequest.templateId, email.templateData.parameters, emailRequest.force, emailRequest.auditData, emailRequest.eventUrl)

    for {
      renderResult <- emailRendererConnector.getTemplatedEmail(sendEmailRequest)
      emailBody     = getEmailBody(renderResult)
      templatedData = EmailTemplateData(sendEmailRequest.templateId, sendEmailRequest.parameters, sendEmailRequest.force, sendEmailRequest.auditData, sendEmailRequest.eventUrl)
      renderedEmail = email.copy(templateData = templatedData, htmlEmailBody = emailBody._1, markdownEmailBody = emailBody._2)
      _            <- draftEmailRepository.persist(renderedEmail)
    } yield renderedEmail
  }

  def sendEmail(emailUUID: String): Future[DraftEmail] = {
    for {
      email      <- draftEmailRepository.getEmailData(emailUUID)
      users      <- callThirdPartyDeveloper(email.userSelectionQuery)
      recipients <- persistInEmailQueue(email, users)
      draftEmail <- draftEmailRepository.updateEmailSentStatus(emailUUID, recipients.size)
    } yield draftEmail
  }

  private def callThirdPartyDeveloper(emailPreferences: DevelopersEmailQuery): Future[List[RegisteredUser]] = {
    implicit val hc = HeaderCarrier()

    val emailPreferencesRedacted = emailPreferences.copy(
      emailsForSomeCases = emailPreferences.emailsForSomeCases
        .map(emailOverride => emailOverride.copy(email = List.empty))
    )
    val numberOfOverrides        = emailPreferences.emailsForSomeCases.map(_.email.size).getOrElse(0)
    logger.info(s"Email Preferences (with overrides redacted) before calling TPD: $emailPreferencesRedacted with $numberOfOverrides overrides")

    val emails = emailPreferences match {
      case DevelopersEmailQuery(None, None, None, false, None, true, None)             =>
        logger.info(s"Emailing All Users scenario.. ")
        developerConnector.fetchVerified()
      case DevelopersEmailQuery(topic, Some(selectedAPIs), None, _, None, false, None) =>
        logger.info(s"Emailing Selected Apis to users that are not overridden")
        val selectedTopic: Option[TopicOptionChoice.Value] = topic.map(TopicOptionChoice.withName)
        if (selectedAPIs.forall(_.isEmpty)) {
          Future.successful(List.empty)
        } else {
          for {
            apis         <- apmConnector.fetchAllCombinedApis()
            filteredApis  = filterSelectedApis(Some(selectedAPIs.toList), apis).sortBy(_.displayName)
            publicUsers  <- handleGettingApiUsers(filteredApis, selectedTopic, PUBLIC)
            privateUsers <- handleGettingApiUsers(filteredApis, selectedTopic, PRIVATE)
            combinedUsers = publicUsers ++ privateUsers
            _             = logger.info(s"Outgoing Emails count is ${combinedUsers.size}")
          } yield combinedUsers
        }
      case DevelopersEmailQuery(_, _, _, _, _, _, Some(EmailOverride(_, false)))       =>
        Future.successful(emailPreferences.emailsForSomeCases.get.email)
      case _                                                                           =>
        logger.info("Getting Emails for Default match case")
        emailPreferences.topic.map(t =>
          developerConnector.fetchByEmailPreferences(TopicOptionChoice.withName(t), emailPreferences.apis, emailPreferences.apiCategories)
        ).getOrElse(Future.successful(List.empty))
    }
    emails
  }

  private def filterSelectedApis(maybeSelectedAPIs: Option[List[String]], apiList: List[CombinedApi]) = {
    maybeSelectedAPIs.fold(List.empty[CombinedApi])(selectedAPIs => apiList.filter(api => selectedAPIs.contains(api.serviceName)))
  }

  private def handleGettingApiUsers(
      apis: List[CombinedApi],
      selectedTopic: Option[TopicOptionChoice.Value],
      apiAcessType: APIAccessType
    )(implicit hc: HeaderCarrier
    ): Future[List[RegisteredUser]] = {
    // APSR-1418 - the accesstype inside combined api is option as a temporary measure until APM version which conatins the change to
    // return this is deployed out to all environments
    logger.info(s"In handleGettingApiUsers  apis: $apis  selectedTopic $selectedTopic apiAccessType ${apiAcessType.toString}")
    val filteredApis = apis.filter(_.accessType.getOrElse(APIAccessType.PUBLIC) == apiAcessType)
    val categories   = filteredApis.flatMap(_.categories.map(toAPICategory))
    val apiNames     = filteredApis.map(_.serviceName)
    selectedTopic.fold(Future.successful(List.empty[RegisteredUser]))(topic => {
      (apiAcessType, filteredApis) match {
        case (_, Nil)     =>
          successful(List.empty[RegisteredUser])
        case (PUBLIC, _)  =>
          logger.debug(s"Before fetchByEmailPreferences topic: $topic  apiNames: $apiNames categories.distinct: ${categories.distinct} privateapimatch: false")
          developerConnector.fetchByEmailPreferences(topic, Some(apiNames), Some(categories.distinct), false)
        case (PRIVATE, _) =>
          logger.debug(s"Before fetchByEmailPreferences topic: $topic  apiNames: $apiNames categories.distinct: ${categories.distinct} privateapimatch: false")
          developerConnector.fetchByEmailPreferences(topic, Some(apiNames), Some(categories.distinct), true)
      }

    })
  }

  private def persistInEmailQueue(email: DraftEmail, users: List[RegisteredUser]): Future[List[EmailRecipient]] = {

    // if sendToActualRecipients is true then actualUsers   + additional recipients
    // if sendToActualRecipients is false  then just  additional recipients
    val usersModified = if (appConfig.sendToActualRecipients) {
      // If an additional recipient is also in the users list, it will get two emails
      users.distinct ++ appConfig.additionalRecipients
    } else {
      appConfig.additionalRecipients
    }

    val sentEmails = usersModified.map(elem =>
      SentEmail(
        createdAt = precise(),
        updatedAt = precise(),
        emailUuid = UUID.fromString(email.emailUUID),
        firstName = elem.firstName,
        lastName = elem.lastName,
        recipient = elem.email,
        status = EmailStatus.PENDING,
        failedCount = 0,
        isUsingInstant = Some(true)
      )
    )

    if (sentEmails.nonEmpty) {
      sentEmailRepository.persist(sentEmails)
    } else {
      logger.warn(s"No Email Addresses selected for sending emails")
    }

    Future.successful(usersModified)
  }

  def fetchEmail(emailUUID: String): Future[DraftEmail] = {
    for {
      email <- draftEmailRepository.getEmailData(emailUUID)
    } yield email
  }

  def deleteEmail(emailUUID: String): Future[Boolean] = {
    draftEmailRepository.deleteByEmailUUID(emailUUID)
  }

  def updateEmail(emailRequest: EmailRequest, emailUUID: String): Future[DraftEmail] = {
    val email: DraftEmail = emailData(emailRequest, emailUUID)

    val sendEmailRequest =
      DraftEmailRequest(emailRequest.userSelectionQuery, emailRequest.templateId, email.templateData.parameters, emailRequest.force, emailRequest.auditData, emailRequest.eventUrl)

    for {
      renderResult <- emailRendererConnector.getTemplatedEmail(sendEmailRequest)
      emailBody     = getEmailBody(renderResult)
      templatedData = EmailTemplateData(sendEmailRequest.templateId, sendEmailRequest.parameters, sendEmailRequest.force, sendEmailRequest.auditData, sendEmailRequest.eventUrl)
      renderedEmail = email.copy(templateData = templatedData, htmlEmailBody = emailBody._1, markdownEmailBody = emailBody._2, subject = emailRequest.emailData.emailSubject)
      _            <- draftEmailRepository.updateEmail(renderedEmail)
    } yield renderedEmail
  }

  private def emailData(emailRequest: EmailRequest, emailUUID: String): DraftEmail = {
    val recipientsTitle                 = "TL API PLATFORM TEAM"
    val parameters: Map[String, String] = Map(
      "subject"        -> s"${emailRequest.emailData.emailSubject}",
      "fromAddress"    -> "gateKeeper",
      "body"           -> s"\n${emailRequest.emailData.emailBody}",
      "service"        -> "gatekeeper",
      "firstName"      -> "((first name))",
      "lastName"       -> "((last name))",
      "showFooter"     -> "false",
      "showHmrcBanner" -> "false"
    )
    val emailTemplateData               = EmailTemplateData(emailRequest.templateId, parameters, emailRequest.force, emailRequest.auditData, emailRequest.eventUrl)

    DraftEmail(
      emailUUID,
      emailTemplateData,
      recipientsTitle,
      emailRequest.userSelectionQuery,
      emailRequest.attachmentDetails,
      emailRequest.emailData.emailBody,
      emailRequest.emailData.emailBody,
      emailRequest.emailData.emailSubject,
      EmailStatus.PENDING,
      "composedBy",
      Some("approvedBy"),
      precise(),
      0,
      isUsingInstant = Some(true)
    )
  }

  private def getEmailBody(rendererResult: Either[UpstreamErrorResponse, RenderResult]) = {
    rendererResult match {
      case Left(UpstreamErrorResponse(message, _, _, _)) =>
        throw new EmailRendererConnectionError(message)
      case Right(result: RenderResult)                   =>
        Tuple2(result.html, result.plain)
    }
  }
}
