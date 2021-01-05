/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.controllers

import javax.inject.{Inject, Singleton}
import org.joda.time.LocalDate
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors.BavfConnector
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.viewmodel.Month
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.PaymentPlanPage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.math.BigDecimal.RoundingMode


@Singleton
class PaymentPlanController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  paymentPlanPage: PaymentPlanPage,
  connector: BavfConnector)
                                     (implicit val appConfig: AppConfig, val serviceConfig: ServicesConfig)
    extends FrontendController(mcc) with I18nSupport {

  val get: Action[AnyContent] = auth.authoriseWithJourneySession { implicit request => vrn => journeySession =>

    def firstPaymentAmount(amountOwed: BigDecimal, periodOwed: Int): BigDecimal = {
      val monthlyAmount = regularPaymentAmount(amountOwed, periodOwed)
      val remainder = amountOwed - (monthlyAmount * periodOwed)
      monthlyAmount + remainder
    }

    def regularPaymentAmount(amountOwed: BigDecimal, periodOwed: Int): BigDecimal = {
      (amountOwed / periodOwed).setScale(2, RoundingMode.DOWN)
    }

    (journeySession.dayOfPayment, journeySession.outStandingAmount) match {
      case (Some(dayOfPayment), Some(outStandingAmount)) => Future.successful(Ok(paymentPlanPage(dayOfPayment, journeySession.numberOfPaymentMonths.getOrElse(11), outStandingAmount, firstPaymentAmount(outStandingAmount,  journeySession.numberOfPaymentMonths.getOrElse(11)), regularPaymentAmount(outStandingAmount,  journeySession.numberOfPaymentMonths.getOrElse(11)))))
      case _ => Future.successful(Redirect(routes.DeferredVatBillController.get()))
    }
  }

  val post: Action[AnyContent] = auth.authoriseWithJourneySession { implicit request =>
    _ =>
      _ =>
        val continueUrl = s"${appConfig.frontendUrl}/bank-details"
        connector.init(continueUrl).map {
          case Some(initResponse) => SeeOther(s"${appConfig.bavfWebBaseUrl}${initResponse.startUrl}")
          case None => InternalServerError
        }
  }
}