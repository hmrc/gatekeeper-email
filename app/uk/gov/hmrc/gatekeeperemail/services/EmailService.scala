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

package uk.gov.hmrc.gatekeeperemail.services

import org.mongodb.scala.result.InsertOneResult
import uk.gov.hmrc.gatekeeperemail.models.Email
import uk.gov.hmrc.gatekeeperemail.repositories.EmailRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailService @Inject()(emailRepository: EmailRepository)
                                           (implicit ec: ExecutionContext) {

  def saveEmail(email: Email): Future[InsertOneResult] = {
    emailRepository.persist(email)
  }
}