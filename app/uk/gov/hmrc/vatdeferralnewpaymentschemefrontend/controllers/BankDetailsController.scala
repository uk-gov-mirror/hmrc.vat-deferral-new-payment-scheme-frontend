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
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors.{BavfConnector, VatDeferralNewPaymentSchemeConnector}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.Bavf.{Account, AccountVerificationAuditWrapper, BusinessCompleteResponse, PersonalCompleteResponse}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.directdebitarrangement.{DirectDebitArrangementRequest, DirectDebitArrangementRequestAuditWrapper}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.services.SessionStore
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.DirectDebitPage

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BankDetailsController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  directDebitPage: DirectDebitPage,
  connector: BavfConnector,
  sessionStore: SessionStore,
  vatDeferralNewPaymentSchemeConnector: VatDeferralNewPaymentSchemeConnector
)(
  implicit ec:ExecutionContext,
  val appConfig: AppConfig,
  val serviceConfig: ServicesConfig,
  auditConnector: AuditConnector
) extends FrontendController(mcc)
  with I18nSupport {

  val logger = Logger(getClass)

  def get(journeyId: String): Action[AnyContent] = auth.authoriseWithJourneySession { implicit request => vrn => journeySession =>
      connector.complete(journeyId).map {
        case Some(r) =>
          audit[AccountVerificationAuditWrapper](
            "bankAccountVerification",
            AccountVerificationAuditWrapper(verified = true, vrn.vrn, Some(r))
          )
          Ok(directDebitPage(journeyId))
        case None =>
          audit[AccountVerificationAuditWrapper](
            "bankAccountVerification",
            AccountVerificationAuditWrapper(verified = false, vrn.vrn, None)
          )
          logger.error(s"bank acount verification failed for ${vrn.vrn}")
          InternalServerError
      }
  }

  def post(journeyId: String): Action[AnyContent] = auth.authoriseWithJourneySession { implicit request => vrn => journeySession =>

    val dayOfPayment:Int =
      journeySession.dayOfPayment.getOrElse(
        throw new IllegalStateException("journeySession missing dayOfPayment")
      )
    val numberOfPaymentMonths:Int =
      journeySession.numberOfPaymentMonths.getOrElse(
        throw new IllegalStateException("journeySession missing numberOfPaymentMonths")
      )
    val outStandingAmount: BigDecimal =
      journeySession.outStandingAmount.getOrElse(
        throw new IllegalStateException("journeySession missing outStandingAmount")
      )
    lazy val ddArrangementAPICall: Future[DirectDebitArrangementRequest] = for {
      x <- connector.complete(journeyId)
    } yield x match {
      case Some(PersonalCompleteResponse(accountOrBusinessName,sortCode,accountNumber)) =>
        DirectDebitArrangementRequest(
          paymentDay = dayOfPayment,
          numberOfPayments = numberOfPaymentMonths,
          totalAmountToPay = outStandingAmount,
          sortCode = sortCode,
          accountNumber = accountNumber,
          accountName = accountOrBusinessName
        )
      case Some(BusinessCompleteResponse(accountOrBusinessName,sortCode,accountNumber)) =>
        DirectDebitArrangementRequest(
          paymentDay = dayOfPayment,
          numberOfPayments = numberOfPaymentMonths,
          totalAmountToPay = outStandingAmount,
          sortCode = sortCode,
          accountNumber = accountNumber,
          accountName = accountOrBusinessName
        )
      case None => throw new Exception("nothing from bank-account-verification-api")
    }

    for {
      a <- ddArrangementAPICall
      _ <- vatDeferralNewPaymentSchemeConnector.createDirectDebitArrangement(vrn.vrn, a)
    } yield {
      audit(
        "directDebitSetup",
        DirectDebitArrangementRequestAuditWrapper(success = true, vrn.vrn, a)
      )
      Redirect(routes.ConfirmationController.get())
    }
  }
}