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
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors.EnrolmentStoreConnector
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.enrolments.{EnrolmentRequest, KnownFacts}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.{DateFormValues, MatchingJourneySession}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.services.SessionStore
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.{EnterVatRegistrationDatePage, VatDetailsNotValidPage}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class VatRegistrationDateController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  sessionStore: SessionStore,
  enrolmentStoreConnector: EnrolmentStoreConnector,
  enterVatRegistrationDatePage: EnterVatRegistrationDatePage,
  vatDetailsNotValidPage: VatDetailsNotValidPage)
  (implicit val appConfig: AppConfig, val serviceConfig: ServicesConfig)
    extends BaseController(mcc) {

  def get(): Action[AnyContent] = auth.authoriseWithMatchingJourneySession { implicit request => matchingJourneySession =>
    Future.successful(Ok(
      enterVatRegistrationDatePage(
        matchingJourneySession.date.fold(frm) (x =>
          frm.fill(DateFormValues(x.day, x.month, x.year))
        )
      )
    ))
  }

  def post(): Action[AnyContent] = auth.authoriseWithMatchingJourneySession { implicit request => matchingJourneySession =>
    frm.bindFromRequest().fold(
      errors => Future(BadRequest(enterVatRegistrationDatePage(errors))),
      formValues => {

        val kf = Seq[KnownFacts](
          KnownFacts("VRN", matchingJourneySession.vrn.getOrElse("")),
          KnownFacts("Postcode", matchingJourneySession.postCode.getOrElse("")))

        val ri = EnrolmentRequest("HMRC-MTD-VAT", kf)


        for {
          journeyState <- sessionStore
                            .store[MatchingJourneySession](
                              matchingJourneySession.id,
                         "MatchingJourneySession",
                              matchingJourneySession.copy(date = Some(formValues))
                            )
          enrolmentResponse <- enrolmentStoreConnector.checkEnrolments(ri) // TODO VDNPS-73
        } yield {
            sessionStore.store[MatchingJourneySession](
              journeyState.id,
              "MatchingJourneySession",
              journeyState.copy(isUserEnrolled = true)
            )
            Redirect(routes.EligibilityController.get())
        }
      }
    )
  }

  val frm: Form[DateFormValues] = Form(
    mapping(
      "day" -> mandatory("day"),
      "month" -> mandatory("month"),
      "year" -> mandatory("year")
    )(DateFormValues.apply)(DateFormValues.unapply)
      .verifying("error.date.invalid", a =>  a.isValidDate)
  )
}