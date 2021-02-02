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
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.MatchingJourneySession
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.services.SessionStore
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.EnterLatestVatPeriodPage

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatPeriodController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  sessionStore: SessionStore,
  enterLatestVatPeriodPage: EnterLatestVatPeriodPage
)(
  implicit val appConfig: AppConfig,
  val serviceConfig: ServicesConfig,
  ec: ExecutionContext
) extends BaseController(mcc) {

  def get(): Action[AnyContent] = auth.authoriseWithMatchingJourneySession { implicit request => matchingJourneySession =>
    Future.successful(Ok(
      enterLatestVatPeriodPage(
        matchingJourneySession.latestAccountPeriodMonth.value.fold(frm){ x =>
          frm.fill(FormValues(x))
        },
        matchingJourneySession.previous
      )
    ))
  }

  def post(): Action[AnyContent] = auth.authoriseWithMatchingJourneySession { implicit request => matchingJourneySession =>
    frm.bindFromRequest().fold(
      errors => Future(BadRequest(enterLatestVatPeriodPage(errors, matchingJourneySession.previous))),
      formValues => {
        sessionStore.store[MatchingJourneySession](
          matchingJourneySession.id,
          "MatchingJourneySession",
          matchingJourneySession.copy(latestAccountPeriodMonth = matchingJourneySession.latestAccountPeriodMonth.copy(value = Some(formValues.month)))
        ).flatMap(_.next)
      }
    )
  }

  val frm: Form[FormValues] = Form(
    mapping("month" -> mandatory("month"))(FormValues.apply)(FormValues.unapply))

  case class FormValues(month: String)
}