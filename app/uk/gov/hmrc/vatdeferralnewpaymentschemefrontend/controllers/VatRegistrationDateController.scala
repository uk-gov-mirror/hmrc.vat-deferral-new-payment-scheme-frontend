/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
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
    extends FrontendController(mcc) with I18nSupport {

  def get(): Action[AnyContent] = auth.authoriseWithMatchingJourneySession { implicit request => matchingJourneySession =>
    Future.successful(Ok(enterVatRegistrationDatePage()))
  }

  def post(): Action[AnyContent] = auth.authoriseWithMatchingJourneySession { implicit request => matchingJourneySession =>

    val form = request.body.asFormUrlEncoded.map { m =>
      m.mapValues(_.last)
    }.flatMap(parseFromMap)

    form match {
      case Some(date) => {

        sessionStore.store[MatchingJourneySession](matchingJourneySession.id, "MatchingJourneySession", matchingJourneySession.copy(date = Some(date)))

        val kf = Seq[KnownFacts] (
          KnownFacts("VRN", matchingJourneySession.vrn.getOrElse("")),
          KnownFacts("Postcode", matchingJourneySession.postCode.getOrElse("")),
          KnownFacts("BoxFiveValue", matchingJourneySession.latestVatAmount.getOrElse("")),
          KnownFacts("LastMonthLatestStagger", matchingJourneySession.latestAccountPeriodMonth.getOrElse("")),
          KnownFacts("VATRegistrationDate", date))

        val ri = RootInterface("HMRC-MTD-VAT", kf)

        enrolmentStoreConnector.checkEnrolments(ri).flatMap { httpResponse =>
          httpResponse.status match {
            case OK => {
              sessionStore.store[MatchingJourneySession](matchingJourneySession.id, "MatchingJourneySession", matchingJourneySession.copy(isUserEnrolled = true))
              Future.successful(Redirect(routes.EligibilityController.get()))
            }
            case 204 => Future.successful(Ok(vatDetailsNotValidPage()))
            case _ => Future.successful(Ok("Api Failed")) // TODO: Add content here
          }
        }
      }
      case None => Future.successful(BadRequest(""))
    }
  }

  private def parseFromMap(in: Map[String, String]): Option[String] = {
    for {
      day <- in.get("day")
      month <- in.get("month")
      year <- in.get("year")
    }
      yield {
        s"$day/$month/$year"
      }
  }
}