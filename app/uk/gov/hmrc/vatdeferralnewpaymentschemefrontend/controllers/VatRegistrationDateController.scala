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

import java.time.{LocalDate, ZoneId}

import javax.inject.{Inject, Singleton}
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors.EnrolmentStoreConnector
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.controllers.enrolments._
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.{DateFormValues, MatchingJourneySession}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.services.SessionStore
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.{EnterVatRegistrationDatePage, VatDetailsNotValidPage}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class VatRegistrationDateController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  sessionStore: SessionStore,
  enrolmentStoreConnector: EnrolmentStoreConnector,
  enterVatRegistrationDatePage: EnterVatRegistrationDatePage,
  vatDetailsNotValidPage: VatDetailsNotValidPage
)(
  implicit val appConfig: AppConfig,
  val serviceConfig: ServicesConfig,
  ec: ExecutionContext
) extends BaseController(mcc) {

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

        for {
          journeyState <- sessionStore.store[MatchingJourneySession](
            matchingJourneySession.id,
            "MatchingJourneySession",
            matchingJourneySession.copy(date = Some(formValues))
          )
          erhmv = enrolmentRequestHmrcMtdVat(matchingJourneySession)
          erhvo = enrolmentRequestHmceVatdecOrg(matchingJourneySession)
          checkOne <- enrolmentStoreConnector.checkEnrolments(erhmv)
          enrolmentResponse <- checkOne.fold(enrolmentStoreConnector.checkEnrolments(erhvo))(x=>Future(Some(x)))
          matched = enrolmentMatches(enrolmentResponse, journeyState)
          _ <- if(matched)
            sessionStore.store[MatchingJourneySession](
              journeyState.id,
              "MatchingJourneySession",
              journeyState.copy(isUserEnrolled = true)
            )
          else
            sessionStore.store[MatchingJourneySession](
              journeyState.id, "MatchingJourneySession",
              journeyState.copy(failedMatchingAttempts = journeyState.failedMatchingAttempts +1)
            )
          result = if (matched)
            Redirect(routes.EligibilityController.get())
          else
            Redirect(routes.NotMatchedController.get())
        } yield result

      }
    )
  }

  def trim(inputStr: String) = inputStr.trim()

  lazy val vatRegDateMapping: Mapping[DateFormValues] = tuple(
    "day"   -> text,
    "month" -> text,
    "year"  -> text
  ).verifying(
    "error.date.emptyfields",
    x =>
      x match {
        case (d: String, m: String, y: String) if trim(d) == "" && trim(m) == "" && trim(y) == "" => false
        case _                                                                                      => true
      })
    .verifying(
      "error.day.missing",
      x =>
        x match {
          case (d: String, m: String, y: String) if trim(d) == "" && trim(m) != "" && trim(y) != "" => false
          case _                                                                                      => true
        })
    .verifying(
      "error.month.missing",
      x =>
        x match {
          case (d: String, m: String, y: String) if trim(d) != "" && trim(m) == "" && trim(y) != "" => false
          case _                                                                                      => true
        })
    .verifying(
      "error.year.missing",
      x =>
        x match {
          case (d: String, m: String, y: String) if trim(d) != "" && trim(m) != "" && trim(y) == "" => false
          case _                                                                                      => true
        })
    .verifying(
      "error.day-and-month.missing",
      x =>
        x match {
          case (d: String, m: String, y: String) if trim(d) == "" && trim(m) == "" && trim(y) != "" => false
          case _                                                                                      => true
        })
    .verifying(
      "error.month-and-year.missing",
      x =>
        x match {
          case (d: String, m: String, y: String) if trim(d) != "" && trim(m) == "" && trim(y) == "" => false
          case _                                                                                      => true
        })
    .verifying(
      "error.day-and-year.missing",
      x =>
        x match {
          case (d: String, m: String, y: String) if trim(d) == "" && trim(m) != "" && trim(y) == "" => false
          case _                                                                                      => true
        })
    .verifying(
      "error.date.invalid",
      x =>
        x match {
          case (d: String, m: String, y: String) if trim(d) != "" && trim(m) != "" && trim(y) != "" =>
            Try(LocalDate.of(trim(y).toInt, trim(m).toInt, trim(d).toInt)).isSuccess
          case _ => true
        }
    )
    .verifying(
      "error.date.in-future",
      x =>
        x match {
          case (d: String, m: String, y: String) if trim(d) != "" && trim(m) != "" && trim(y) != "" =>
            LocalDate.of(trim(y).toInt, trim(m).toInt, trim(d).toInt)
              .isBefore(LocalDate.now(ZoneId.of("Europe/London")))
          case _ => true
        }
    )
    .transform(
      { case (d, m, y) => DateFormValues(d,m,y) },
      duy => (duy.day, duy.month, duy.year)
    )

  val frm: Form[DateFormValues] = Form(
    vatRegDateMapping
      .verifying("error.date.invalid", a =>  a.isValidDate)
  )
}