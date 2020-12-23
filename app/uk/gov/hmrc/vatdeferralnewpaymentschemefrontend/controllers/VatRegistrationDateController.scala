/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.controllers

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors.EnrolmentStoreConnector
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.{KnownFacts, MatchingJourneySession, RootInterface}
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

  def get(): Action[AnyContent] = auth.authoriseForMatchingJourney { implicit request =>
    Future.successful(Ok(enterVatRegistrationDatePage(frm)))
  }

  def post(): Action[AnyContent] = auth.authoriseWithMatchingJourneySession { implicit request => matchingJourneySession =>
    frm.bindFromRequest().fold(
      errors => Future(BadRequest(enterVatRegistrationDatePage(errors))),
      formValues => {
        sessionStore.store[MatchingJourneySession](matchingJourneySession.id, "MatchingJourneySession", matchingJourneySession.copy(date = Some(formValues.day)))

        val kf = Seq[KnownFacts](
          KnownFacts("VRN", matchingJourneySession.vrn.getOrElse("")),
          KnownFacts("Postcode", matchingJourneySession.postCode.getOrElse("")),
          KnownFacts("BoxFiveValue", matchingJourneySession.latestVatAmount.getOrElse("")),
          KnownFacts("LastMonthLatestStagger", matchingJourneySession.latestAccountPeriodMonth.getOrElse("")),
          KnownFacts("VATRegistrationDate", formValues.day))

        val ri = RootInterface("HMRC-MTD-VAT", kf)

        enrolmentStoreConnector.checkEnrolments(ri).flatMap { httpResponse =>
          httpResponse.status match {
            case OK => {
              sessionStore.store[MatchingJourneySession](matchingJourneySession.id, "MatchingJourneySession", matchingJourneySession.copy(isUserEnrolled = true))
              Future.successful(Redirect(routes.EligibilityController.get()))
            }
            case 204 => {
              sessionStore.store[MatchingJourneySession](matchingJourneySession.id, "MatchingJourneySession", matchingJourneySession.copy(failedMatchingAttempts = matchingJourneySession.failedMatchingAttempts + 1))
              Future.successful(Redirect(routes.NotMatchedController.get()))
            }
          }
        }
      }
    )
  }

  val frm: Form[FormValues] = Form(
    mapping(
      "day" -> mandatory("day"),
      "month" -> mandatory("month"),
      "year" -> mandatory("year")
    )(FormValues.apply)(FormValues.unapply)
      .verifying("error.date.invalid", a =>  a.isValidDate)
  )

  case class FormValues(day: String, month: String, year: String) {
    def isValidDate = try{
      LocalDate.parse(s"$day/$month/$year", DateTimeFormatter.ofPattern("dd/MM/yyyy"))
      true
    }
    catch {
      case _ => false
    }
  }
}