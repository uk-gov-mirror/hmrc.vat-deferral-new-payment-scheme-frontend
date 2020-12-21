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
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.MatchingJourneySession
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.services.SessionStore
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.EnterLatestVatPeriodPage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class VatPeriodController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  sessionStore: SessionStore,
  enterLatestVatPeriodPage: EnterLatestVatPeriodPage)
  (implicit val appConfig: AppConfig, val serviceConfig: ServicesConfig)
    extends FrontendController(mcc) with I18nSupport {

  def get(): Action[AnyContent] = auth.authoriseWithMatchingJourneySession { implicit request => matchingJourneySession =>
    Future.successful(Ok(enterLatestVatPeriodPage()))
  }

  def post(): Action[AnyContent] = auth.authoriseWithMatchingJourneySession { implicit request => matchingJourneySession =>

    val form = request.body.asFormUrlEncoded.map { m =>
      m.mapValues(_.last)
    }.flatMap(parseFromMap)

    form match {
      case Some(month) => {
        sessionStore.store[MatchingJourneySession](matchingJourneySession.id, "MatchingJourneySession", matchingJourneySession.copy(latestAccountPeriodMonth = Some(month)))
        Future.successful(Redirect(routes.VatRegistrationDateController.get()))
      }
      case None => Future.successful(BadRequest(""))
    }
  }

  private def parseFromMap(in: Map[String, String]): Option[String] = {
    for {month <- in.get("month")}
      yield {
        month
      }
  }
}