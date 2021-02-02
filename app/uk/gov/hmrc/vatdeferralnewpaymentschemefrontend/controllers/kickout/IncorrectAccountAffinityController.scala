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

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.controllers.kickout


import javax.inject._
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, MessagesControllerComponents}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.errors.IncorrectAccountAffinityPage

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncorrectAccountAffinityController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  incorrectAccountAffinityPage: IncorrectAccountAffinityPage
)(
  implicit val appConfig: AppConfig,
  ec: ExecutionContext
) extends FrontendController(mcc)
  with I18nSupport {

  def get(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(incorrectAccountAffinityPage()))
  }

}
