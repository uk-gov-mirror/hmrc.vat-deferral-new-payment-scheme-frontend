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

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneId}

import javax.inject.{Inject, Singleton}
import play.api.data.{Form, Mapping}
import play.api.mvc._
import play.api.data.Forms._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors.EnrolmentStoreConnector
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.enrolments.{EnrolmentRequest, EnrolmentResponse, Identifiers, KnownFacts}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.{DateFormValues, MatchingJourneySession}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.services.SessionStore
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.{EnterVatRegistrationDatePage, VatDetailsNotValidPage}

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
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

  private val HmrcMtdVatService = "HMRC-MTD-VAT"
  private val HmceVatdecOrgService = "HMCE-VATDEC-ORG"

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

        val enrolmentRequestHmrcMtdVat =
          EnrolmentRequest(
            HmrcMtdVatService,
            Seq[KnownFacts](
              KnownFacts("VRN", matchingJourneySession.vrn.getOrElse("")),
              KnownFacts("Postcode", matchingJourneySession.postCode.getOrElse("")))
          )

        val enrolmentRequestHmceVatdecOrg =
          EnrolmentRequest(
            HmceVatdecOrgService,
            Seq[KnownFacts](
              KnownFacts("VATRegNo", matchingJourneySession.vrn.getOrElse("")),
              KnownFacts("IRPCODE", matchingJourneySession.postCode.getOrElse("")))
          )

        for {
          journeyState <- sessionStore.store[MatchingJourneySession](
            matchingJourneySession.id,
            "MatchingJourneySession",
            matchingJourneySession.copy(date = Some(formValues))
          )
          checkOne <- enrolmentStoreConnector.checkEnrolments(enrolmentRequestHmrcMtdVat)
          enrolmentResponse <- checkOne.fold(enrolmentStoreConnector.checkEnrolments(enrolmentRequestHmceVatdecOrg))(x=>Future(Some(x)))
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


  // TODO: Refactor this code and sanitize enrolment store data
  private def enrolmentMatches(enrolmentResponse: Option[EnrolmentResponse], journeyState: MatchingJourneySession):Boolean = {
    enrolmentResponse match {
      case Some(er) if er.enrolments.isEmpty =>
        false
      case Some(er) if er.enrolments.length > 1 =>
        // TODO: How should we handle multiple enrolments?
        throw new RuntimeException("Too many enrolments returned from the enrolment store, which one should we use?")
      case Some(er) if er.service == HmrcMtdVatService => {
        val items: List[Boolean] = er.enrolments.head.verifiers.map { a =>
          if (a.key == "BoxFiveValue") {
            a.value == journeyState.latestVatAmount.getOrElse("")
          }
          else if (a.key == "LastMonthLatestStagger") {
            a.value == journeyState.latestAccountPeriodMonth.getOrElse("")
          }
          else if (a.key == "VATRegistrationDate") {
            val dt = journeyState.date.getOrElse(throw new RuntimeException(""))
            a.value == s"${"%02d".format(dt.day.toInt)}/${"%02d".format(dt.month.toInt)}/${dt.year.takeRight(2)}"
          }
          else true
        }
        !items.contains(false)
      }
      case Some(er) if er.service == HmceVatdecOrgService => {
        val items: List[Boolean] = er.enrolments.head.verifiers.map { a =>
          if (a.key == "PETAXDUESALES") {
            a.value == journeyState.latestVatAmount.getOrElse("")
          }
          else if (a.key == "PEPDNO") {
            a.value == journeyState.latestAccountPeriodMonth.fold(""){x =>
              LocalDate.now.withMonth(Integer.parseInt(x))
                .format(DateTimeFormatter.ofPattern("MMM")).toLowerCase()
            }
          }
          else if (a.key == "IREFFREGDATE") {
            val dt = journeyState.date.getOrElse(throw new RuntimeException(""))
            a.value == s"${"%02d".format(dt.day.toInt)}/${"%02d".format(dt.month.toInt)}/${dt.year.takeRight(2)}"
          }
          else true
        }
        !items.contains(false)
      }
      case _ =>
        false
    }
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