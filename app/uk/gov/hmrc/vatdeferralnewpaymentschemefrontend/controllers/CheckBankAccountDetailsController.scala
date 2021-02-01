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
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors.BavfConnector
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.Bavf._
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.services.SessionStore
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.{CheckBeforeYouStartPage, CheckYourDirectDebitDetailsPage}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckBankAccountDetailsController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  connector: BavfConnector,
  sessionStore: SessionStore,
  checkYourDirectDebitDetailsPage: CheckYourDirectDebitDetailsPage
)(
  implicit val appConfig: AppConfig,
  val serviceConfig: ServicesConfig,
  ec: ExecutionContext,
  auditConnector: AuditConnector
) extends FrontendController(mcc)
  with I18nSupport {

  val logger = Logger(getClass)

  def get(journeyId: String): Action[AnyContent] = auth.authorise { implicit request => implicit vrn =>
    def auditSuccess(account: Option[Account]): Unit = audit[AccountVerificationAuditWrapper](
      "bankAccountVerification",
      AccountVerificationAuditWrapper(verified = true, vrn.vrn, account)
    )

    def auditFailure(errLogMsg: String, account: Option[Account] = None): Unit = {
      audit[AccountVerificationAuditWrapper](
        "bankAccountVerification",
        AccountVerificationAuditWrapper(verified = false, vrn.vrn, account)
      )
      logger.error(errLogMsg)
    }
    connector.complete(journeyId).map {

      case Some(r) =>
        println(s"$r AAAAAAAAAAAAAA")
        r match {
          case PersonalCompleteResponse(accountOrBusinessName,sortCode,accountNumber, Some(reputationResponseEnum)) =>
            reputationResponseEnum match {
              case ReputationResponseEnum.Yes =>
                auditSuccess(Some(r))
                //DD confirmation page for Yes reputationResponse from BAVF
                Redirect(routes.BankDetailsController.get(journeyId))
              case ReputationResponseEnum.Indeterminate =>
                auditSuccess(Some(r))
                //cya page due to indeterminate reputationResponse
                Ok(
                  checkYourDirectDebitDetailsPage(
                    "personal",
                    accountOrBusinessName,
                    sortCode,
                    accountNumber,
                    journeyId
                  )
                )
              case _ =>
                //Redirect back to BAVF journey due to error
                auditFailure(s"reputationResponse is not valid for ${vrn.vrn}, redirecting to start of BAVF journey", Some(r))
                Redirect(routes.PaymentPlanController.post())
            }
          case BusinessCompleteResponse(accountOrBusinessName,sortCode,accountNumber, Some(reputationResponseEnum)) =>
            reputationResponseEnum match {
              case ReputationResponseEnum.Yes =>
                //DD confirmation page for Yes reputationResponse from BAVF
                auditSuccess(Some(r))
                Redirect(routes.BankDetailsController.get(journeyId))
              case ReputationResponseEnum.Indeterminate =>
                //cya page due to indeterminate reputationResponse from BAVF
                auditSuccess(Some(r))
                Ok(
                  checkYourDirectDebitDetailsPage(
                    "business",
                    accountOrBusinessName,
                    sortCode,
                    accountNumber,
                    journeyId
                  )
                )
              case _ =>
                //Redirect back to BAVF journey due to error
                auditFailure(s"reputationResponse is not valid for ${vrn.vrn}, redirecting to start of BAVF journey", Some(r))
                Redirect(routes.PaymentPlanController.post())
            }
          case _ =>
            auditFailure(s"No reputationResponse returned from BAVF for ${vrn.vrn}")
            Redirect(routes.PaymentPlanController.post()) //Redirect back to BAVF journey due to error
        }

      case None =>
        auditFailure(s"bank acount verification failed for ${vrn.vrn}")
        InternalServerError
    }
  }
}
