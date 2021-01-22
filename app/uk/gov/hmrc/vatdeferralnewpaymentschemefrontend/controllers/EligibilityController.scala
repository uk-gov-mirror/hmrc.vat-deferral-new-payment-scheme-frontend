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

import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors.VatDeferralNewPaymentSchemeConnector
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.{Eligibility, JourneySession}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.services.SessionStore
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.{NotEligiblePage, ReturningUserPage}

import scala.concurrent.ExecutionContext

@Singleton
class EligibilityController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  vatDeferralNewPaymentSchemeConnector: VatDeferralNewPaymentSchemeConnector,
  sessionStore: SessionStore,
  notEligiblePage: NotEligiblePage,
  returningUserPage: ReturningUserPage
)(
  implicit val appConfig: AppConfig,
  val serviceConfig: ServicesConfig,
  ec: ExecutionContext
) extends FrontendController(mcc)
  with I18nSupport {

  val get: Action[AnyContent] = auth.authorise { implicit request =>
    implicit vrn =>
      vatDeferralNewPaymentSchemeConnector.eligibility(vrn.vrn) map {

        case Eligibility(false, false, true) =>
          request.session.get("sessionId").map{ sessionId =>
            sessionStore.store[JourneySession](sessionId, "JourneySession", JourneySession(sessionId, true))
            Redirect(routes.CheckBeforeYouStartController.get())
          }.getOrElse(InternalServerError)

        case Eligibility(true, _ , _) =>
          Ok(returningUserPage())

        case e => Ok(notEligiblePage(e))
      }
  }
}