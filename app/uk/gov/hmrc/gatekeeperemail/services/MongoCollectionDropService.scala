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

package uk.gov.hmrc.gatekeeperemail.services

import uk.gov.hmrc.gatekeeperemail.repositories.EmailRepository
import uk.gov.hmrc.gatekeeperemail.util.ApplicationLogger

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class MongoCollectionDropService@Inject()(emailRepository: EmailRepository)
                                         (implicit val ec: ExecutionContext) extends ApplicationLogger {
  try{
    logger.info("Going to drop Index...")
    emailRepository.collection.dropIndex("emailUUIDIndex")
  }
  catch{
    case e : Exception => logger.info(s"Exception thrown is ${e.getMessage}")
  }

}
