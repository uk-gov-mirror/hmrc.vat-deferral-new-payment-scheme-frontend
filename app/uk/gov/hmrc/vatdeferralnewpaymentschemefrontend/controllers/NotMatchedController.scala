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
import play.api.{Configuration, Environment, Logger, Mode}
import play.api.i18n.I18nSupport
import play.api.mvc.{request, _}
import uk.gov.hmrc.play.bootstrap.config.{AuthRedirects, ServicesConfig}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.MatchingJourneySession
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.services.SessionStore
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.errors.NotMatchedPage

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotMatchedController @Inject()(
  mcc: MessagesControllerComponents,
  notMatchedPage: NotMatchedPage,
  sessionStore: SessionStore,
  val env: Environment,
  val config: Configuration
)(
  implicit val appConfig: AppConfig,
  val serviceConfig: ServicesConfig,
  ec: ExecutionContext
)
  extends FrontendController(mcc)
  with I18nSupport
  with AuthRedirects
{
  val logger = Logger(getClass)

    def get: Action[AnyContent] = Action.async { implicit request =>
      val currentUrl = {
        if (env.mode.equals(Mode.Dev)) s"http://${request.host}${request.uri}" else s"${request.uri}"
      }
      request.session.get("sessionId").fold(Future.successful(toGGLogin(currentUrl))){sessionId =>
      sessionStore.get[MatchingJourneySession](sessionId, "MatchingJourneySession").map { x =>
        x.fold{
          logger.warn(s"sessionStore cannot be retrieved for $sessionId")
          Redirect(routes.VrnController.get())
        }{ y =>
          Ok(
            notMatchedPage(y.failedMatchingAttempts, y.locked)
          )
        }
      }
    }
  }
}
