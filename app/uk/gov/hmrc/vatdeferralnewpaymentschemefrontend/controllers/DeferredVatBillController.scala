/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.DeferredVatBillPage
import play.api.i18n.I18nSupport
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.{ FinancialData }
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors.VatDeferralNewPaymentSchemeConnector
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.services.SessionStore
import scala.concurrent.Future

@Singleton
class DeferredVatBillController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  vatDeferralNewPaymentSchemeConnector: VatDeferralNewPaymentSchemeConnector,
  deferredVatBillPage: DeferredVatBillPage,
  sessionStore: SessionStore)
  (implicit val appConfig: AppConfig, val serviceConfig: ServicesConfig)
    extends FrontendController(mcc) with I18nSupport {

  def get(): Action[AnyContent] = auth.authorise { implicit request => vrn =>

    request.session.get("sessionId").map(sessionId => {
      vatDeferralNewPaymentSchemeConnector.financialData(vrn.vrn) map {
        case e@FinancialData(_, _) => {
          sessionStore.store[String](sessionId, "amount", e.outstandingAmount.toString)
          Ok(deferredVatBillPage(e.originalAmount.toString, e.outstandingAmount.toString, (e.originalAmount - e.outstandingAmount).toString))
        }
        case _ => Ok("Financial data issue")
      }
    }).getOrElse(Future.successful(Ok("Session id not set")))
  }
}
