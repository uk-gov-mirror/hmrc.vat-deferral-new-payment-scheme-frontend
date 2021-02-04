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
    def auditSuccess(warnLogMsg: Option[String] = None, account: Option[Account] = None): Unit = {
      audit[AccountVerificationAuditWrapper](
        "bankAccountVerification",
        AccountVerificationAuditWrapper(verified = true, vrn.vrn, account)
      )
      warnLogMsg.foreach { wlm =>
        logger.warn(wlm)
      }
    }

    def auditFailure(errLogMsg: String, account: Option[Account] = None): Unit = {
      audit[AccountVerificationAuditWrapper](
        "bankAccountVerification",
        AccountVerificationAuditWrapper(verified = false, vrn.vrn, account)
      )
      logger.error(errLogMsg)
    }
    connector.complete(journeyId).map {

      case Some(r) =>
        r match {
          case PersonalCompleteResponse(accountOrBusinessName, sortCode, accountNumber, Some(reputationResponseEnum)) =>
            reputationResponseEnum match {
              case _=>
                auditSuccess(account = Some(r))
                //cya page due to Yes reputationResponse from BAVF
                Ok(
                  checkYourDirectDebitDetailsPage(
                    "personal",
                    accountOrBusinessName,
                    sortCode,
                    accountNumber,
                    journeyId
                  )
                )
              case ReputationResponseEnum.Indeterminate | ReputationResponseEnum.Inapplicable =>
                //Log warning and go direct to DD confirmation page
                auditSuccess(
                  warnLogMsg = Some(
                    "reputationResponseEnum was determined to be Indeterminate or Inapplicable, " +
                      "continuing with journey as data may be valid"
                  ),
                  account = Some(r)
                )
                Redirect(routes.BankDetailsController.get(journeyId))
              case ReputationResponseEnum.No =>
                //Redirect back to BAVF journey as accountNumber is not valid for sortCode
                auditFailure("reputationResponseEnum indicates accountNumber is not valid for sortCode", Some(r))
                Redirect(routes.PaymentPlanController.post())
              case ReputationResponseEnum.Error =>
                //Error may be due to third party connection issue, log error and continue with journey
                auditFailure(
                  s"reputationResponseEnum has returned error for ${vrn.vrn}, " +
                    s"continuing with journey as data may be valid", Some(r)
                )
                Redirect(routes.BankDetailsController.get(journeyId))
            }
          case BusinessCompleteResponse(accountOrBusinessName, sortCode, accountNumber, Some(reputationResponseEnum)) =>
            reputationResponseEnum match {
              case ReputationResponseEnum.Yes =>
                auditSuccess(account = Some(r))
                //cya page due to Yes reputationResponse from BAVF
                Ok(
                  checkYourDirectDebitDetailsPage(
                    "business",
                    accountOrBusinessName,
                    sortCode,
                    accountNumber,
                    journeyId
                  )
                )
              case ReputationResponseEnum.Indeterminate | ReputationResponseEnum.Inapplicable =>
                //Log warning and go direct to DD confirmation page
                auditSuccess(
                  warnLogMsg = Some(
                    "reputationResponseEnum was determined to be Indeterminate or Inapplicable, " +
                      "continuing with journey as data may be valid"
                  ),
                  account = Some(r)
                )
                Redirect(routes.BankDetailsController.get(journeyId))
              case ReputationResponseEnum.No =>
                //Redirect back to BAVF journey as accountNumber is not valid for sortCode
                auditFailure("reputationResponseEnum indicates accountNumber is not valid for sortCode", Some(r))
                Redirect(routes.PaymentPlanController.post())
              case ReputationResponseEnum.Error =>
                //Error may be due to third party connection issue, log error and continue with journey
                auditFailure(
                  s"reputationResponseEnum has returned error for ${vrn.vrn}, " +
                    s"continuing with journey as data may be valid", Some(r)
                )
                Redirect(routes.BankDetailsController.get(journeyId))
            }
          case _ =>
            auditFailure(s"No Account returned from BAVF for ${vrn.vrn}")
            Redirect(routes.PaymentPlanController.post()) //Redirect back to BAVF journey due to error
        }

      case None =>
        auditFailure(s"No response from BAVF, assume bank account verification failed for ${vrn.vrn}")
        InternalServerError
    }
  }

  def callBavfInit(journeyId: String): Action[AnyContent] = auth.authorise { implicit request => implicit vrn =>
    val continueUrl = s"${appConfig.frontendUrl}/check-the-account-details"

    lazy val bavfApiCall = for {
      x <- connector.complete(journeyId)
    } yield x match {
      case Some(PersonalCompleteResponse(accountOrBusinessName,sortCode,accountNumber, _)) =>
        InitRequestPrepopulatedData(AccountTypeRequestEnum.Personal, Some(accountOrBusinessName), Some(sortCode), Some(accountNumber))
      case Some(BusinessCompleteResponse(accountOrBusinessName,sortCode,accountNumber, _)) =>
        InitRequestPrepopulatedData(AccountTypeRequestEnum.Business, Some(accountOrBusinessName), Some(sortCode), Some(accountNumber))
      case None => throw new Exception("nothing from bank-account-verification-api")
    }
    bavfApiCall.flatMap { ppd =>
        connector.init(continueUrl, prepopulatedData = Some(ppd)).map {
          case Some(initResponse) =>
            SeeOther(s"${appConfig.bavfWebBaseUrl}${initResponse.startUrl}")
          case None => InternalServerError
        }
    }
  }
}
