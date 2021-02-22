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

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.controllers

import play.api.{Configuration, Environment}
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.{JourneySession, MatchingJourneySession, Vrn}

import scala.concurrent.{ExecutionContext, Future}

class FakeAuth(val authConnector: AuthConnector, val env: Environment, val config: Configuration) extends Auth {
  override def authorise(
    action: Request[AnyContent] => Vrn => Future[Result]
  )(
    implicit ec: ExecutionContext,
    servicesConfig: ServicesConfig
  ): Action[AnyContent] =
    Action.async { implicit request =>
      action(request)(Vrn("9999999999"))
    }

  override def authoriseWithJourneySession(
    action: Request[AnyContent] => Vrn => JourneySession => Future[Result]
  )(
    implicit ec: ExecutionContext,
    servicesConfig: ServicesConfig
  ): Action[AnyContent] =
    Action.async { implicit request =>
      action(request)(Vrn("9999999999"))(JourneySession("foo", true, Some(BigDecimal(100.00)), Some(11),Some(1)))
    }

  override def authoriseForMatchingJourney(action: Request[AnyContent] => Future[Result])(implicit ec: ExecutionContext, servicesConfig: ServicesConfig): Action[AnyContent] = ???

  override def authoriseWithMatchingJourneySession(action: Request[AnyContent] => MatchingJourneySession => Future[Result])(implicit ec: ExecutionContext, servicesConfig: ServicesConfig): Action[AnyContent] = ???
}