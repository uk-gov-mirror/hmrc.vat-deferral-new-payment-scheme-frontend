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
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors.{BavfConnector, VatDeferralNewPaymentSchemeConnector}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.Bavf._
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.directdebitarrangement.{DirectDebitArrangementRequest, DirectDebitArrangementRequestAuditWrapper}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.{JourneySession, Submission}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.services.SessionStore
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.DirectDebitPage
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.errors.DDFailurePage

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BankDetailsController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  directDebitPage: DirectDebitPage,
  ddFailurePage: DDFailurePage,
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
    Future.successful(Ok(directDebitPage(journeyId)))
  }

  def post(journeyId: String): Action[AnyContent] = auth.authoriseWithJourneySession { implicit request =>
    vrn =>
      journeySession =>

        val dayOfPayment: Int =
          journeySession.dayOfPayment.getOrElse(
            throw new IllegalStateException("journeySession missing dayOfPayment")
          )
        val numberOfPaymentMonths: Int =
          journeySession.numberOfPaymentMonths.getOrElse(
            throw new IllegalStateException("journeySession missing numberOfPaymentMonths")
          )
        val outStandingAmount: BigDecimal =
          journeySession.outStandingAmount.getOrElse(
            throw new IllegalStateException("journeySession missing outStandingAmount")
          )
        lazy val ddArrangementAPICall: Future[DirectDebitArrangementRequest] = for {
          account <- connector.complete(journeyId, vrn.vrn)
        } yield account match {
          case PersonalCompleteResponse(accountOrBusinessName, sortCode, accountNumber, _) =>
            DirectDebitArrangementRequest(
              paymentDay = dayOfPayment,
              numberOfPayments = numberOfPaymentMonths,
              totalAmountToPay = outStandingAmount,
              sortCode = sortCode,
              accountNumber = accountNumber,
              accountName = accountOrBusinessName
            )
          case BusinessCompleteResponse(accountOrBusinessName, sortCode, accountNumber, _) =>
            DirectDebitArrangementRequest(
              paymentDay = dayOfPayment,
              numberOfPayments = numberOfPaymentMonths,
              totalAmountToPay = outStandingAmount,
              sortCode = sortCode,
              accountNumber = accountNumber,
              accountName = accountOrBusinessName
            )
        }

        def storeSubmitted(journeySession: JourneySession, sub: Submission): Future[JourneySession] = {
          sessionStore.store[JourneySession](
            journeySession.id,
            "JourneySession",
            journeySession.copy(submission = sub)
          )
        }

        def handleSubmission(submission: Submission, i: Int)(implicit request: Request[AnyContent]): Future[Result] = (submission,i) match {
          case (_, count) if count > 30 =>
            throw new RuntimeException("too many retries in double-click prevention")
          case (Submission(true, "error", _, errorMsg, errorCode),_) =>
            throw UpstreamErrorResponse(errorMsg, errorCode, errorCode)
          case (Submission(true, "redirect", b, _, _),_) =>
            Future.successful(Redirect(b))
          case (Submission(true, "ok", _, _, _),_) =>
            Future.successful(Ok(ddFailurePage()))
          case (Submission(true, "", "", "", _),_) =>
            sessionStore.get[JourneySession](journeySession.id, "JourneySession")
              .flatMap(x => x.fold(throw new RuntimeException("Unable to get JourneySession from state")){ y =>
                Thread.sleep(1000)
                handleSubmission(y.submission, i +1)
              })
          case _ => throw new RuntimeException("No match in double-click prevention")
        }

        journeySession.submission match {
          case Submission(false, _, _, _, _) => {
            for {
              _ <- storeSubmitted(journeySession, Submission(isSubmitted = true))
              a <- ddArrangementAPICall
              b <- vatDeferralNewPaymentSchemeConnector.createDirectDebitArrangement(vrn.vrn, a)
            } yield {
              (a, b) match {
                case (_, Right(_)) =>
                  storeSubmitted(journeySession, Submission(isSubmitted = true, "redirect", routes.ConfirmationController.get().url))
                  audit(
                    "DirectDebitSetup",
                    DirectDebitArrangementRequestAuditWrapper(success = true, vrn.vrn, a)
                  )
                  Redirect(routes.ConfirmationController.get())
                case (_, Left(UpstreamErrorResponse(message, 406, _, _))) =>
                  audit(
                    "DirectDebitSetup",
                    DirectDebitArrangementRequestAuditWrapper(success = false, vrn.vrn, a)
                  )
                  logger.info(s"getting 406, message: $message")
                  storeSubmitted(journeySession, Submission(isSubmitted = true, "ok"))
                   Ok(ddFailurePage())
                case (_, Left(e)) =>
                  logger.error(e.message)
                  storeSubmitted(journeySession, Submission(isSubmitted = true, "error", e.message))
                  audit(
                    "DirectDebitSetup",
                    DirectDebitArrangementRequestAuditWrapper(success = false, vrn.vrn, a)
                  )
                  throw e
              }
            }
          }
          case submission@Submission(_,_,_,_,_) =>
            handleSubmission(submission, 0)
        }
  }
}