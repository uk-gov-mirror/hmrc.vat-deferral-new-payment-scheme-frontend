/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.HowManyMonthsPage
import scala.concurrent.Future
import play.api.i18n.I18nSupport
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.viewmodel.Month
import scala.math.BigDecimal.RoundingMode
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.services.SessionStore

@Singleton
class MonthsController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  howManyMonthsPage: HowManyMonthsPage,
  sessionStore: SessionStore)
  (implicit val appConfig: AppConfig, val serviceConfig: ServicesConfig)
    extends FrontendController(mcc) with I18nSupport {

  val get: Action[AnyContent] = auth.authorise { implicit request =>
    implicit vrn =>
      request.session.get("sessionId").map(sessionId => {
        sessionStore.get[BigDecimal](sessionId, "amount").map {
          case Some(a) => Ok(howManyMonthsPage(getMonths(a)))
          case _ => Redirect(routes.DeferredVatBillController.get())
        }
      }).getOrElse(Future.successful(Ok("Session id not set")))
  }

  def getMonths(amount: BigDecimal): Seq[Month] = {
    (2 to 11).map {
      month => {
        val monthlyAmount = (amount / month).setScale(2, RoundingMode.DOWN)
        val remainder = amount - (monthlyAmount * month)
        Month(month.toString, monthlyAmount.toString, remainder.toString)
      }
    }
  }
}
