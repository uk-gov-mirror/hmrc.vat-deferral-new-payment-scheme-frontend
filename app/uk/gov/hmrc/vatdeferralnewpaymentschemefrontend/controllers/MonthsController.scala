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
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.http.{HttpResponse, NotFoundException}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.Vrn
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.viewmodel.Month
import scala.collection.mutable.ListBuffer
import scala.math.BigDecimal.RoundingMode

@Singleton
class MonthsController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  howManyMonthsPage: HowManyMonthsPage)
  (implicit val appConfig: AppConfig, val serviceConfig: ServicesConfig)
    extends FrontendController(mcc) with I18nSupport {

  val get: Action[AnyContent] = auth.authorise { implicit request => implicit vrn =>

    val amount = request.session.get("amount").getOrElse("0")

    var months = (2 to 11).map {
      month => {
        val monthlyAmount = (BigDecimal(amount) / month).setScale(2, RoundingMode.DOWN)
        val remainder = BigDecimal(amount) - (monthlyAmount * month)
        Month(month.toString, monthlyAmount.toString, remainder.toString)
      }
    }

    Future.successful(Ok(howManyMonthsPage(months)))
  }
}