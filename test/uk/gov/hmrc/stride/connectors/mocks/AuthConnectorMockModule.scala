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

package uk.gov.hmrc.apiplatform.modules.stride.connectors.mocks

import java.util.UUID
import scala.concurrent.Future.{failed, successful}

import org.mockito.ArgumentMatchers.any as `*`
import org.mockito.Mockito.{when, withSettings}
import org.mockito.quality.Strictness
import org.scalatestplus.mockito.MockitoSugar

import uk.gov.hmrc.auth.core.retrieve.{Name, Retrieval, ~}
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments, InsufficientEnrolments, SessionRecordNotFound}
import uk.gov.hmrc.gatekeeperemail.stride.connectors.AuthConnector

trait AuthConnectorMockModule extends MockitoSugar {

  trait BaseAuthConnectorMock {
    def aMock: AuthConnector

    val userName      = "userName"
    val superUserName = "superUserName"
    val adminName     = "adminName"

    val userRole      = s"userRole${UUID.randomUUID}"
    val adminRole     = s"adminRole${UUID.randomUUID}"
    val superUserRole = s"superUserRole${UUID.randomUUID}"

    object Authorise {

      def thenReturn() = {
        val response = successful(new ~(Some(Name(Some(adminName), None)), Enrolments(Set(Enrolment(adminRole)))))

        when(aMock.authorise(*, *[Retrieval[~[Option[Name], Enrolments]]])(using *, *)).thenReturn(response)
      }

      def thenReturnInsufficientEnrolments() = {
        when(aMock.authorise(*, *[Retrieval[~[Option[Name], Enrolments]]])(using *, *)).thenReturn(failed(new InsufficientEnrolments))
      }

      def thenReturnSessionRecordNotFound() = {
        when(aMock.authorise(*, *[Retrieval[~[Option[Name], Enrolments]]])(using *, *)).thenReturn(failed(new SessionRecordNotFound))
      }

      def thenReturnNoName() = {
        val response = successful(new ~(Option.empty[Name], Enrolments(Set(Enrolment(adminRole)))))

        when(aMock.authorise(*, *[Retrieval[~[Option[Name], Enrolments]]])(using *, *)).thenReturn(response)
      }
    }
  }

  object AuthConnectorMock extends BaseAuthConnectorMock {
    val aMock = mock[AuthConnector](withSettings().strictness(Strictness.LENIENT))
  }
}
