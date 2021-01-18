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
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors.{BavfConnector, VatDeferralNewPaymentSchemeConnector}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.Bavf.{BusinessCompleteResponse, PersonalCompleteResponse}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.directdebitarrangement.DirectDebitArrangementRequest
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.services.SessionStore
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.DirectDebitPage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

@Singleton
class BankDetailsController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  directDebitPage: DirectDebitPage,
  connector: BavfConnector,
  sessionStore: SessionStore,
  vatDeferralNewPaymentSchemeConnector: VatDeferralNewPaymentSchemeConnector)
  (implicit val appConfig: AppConfig, val serviceConfig: ServicesConfig)
  extends FrontendController(mcc) with I18nSupport {

  def get(journeyId: String): Action[AnyContent] = auth.authoriseWithJourneySession { implicit request => vrn => journeySession =>
      connector.complete(journeyId).map {
        case Some(r) => Ok(directDebitPage(journeyId))
        case None => InternalServerError
      }
  }

  def post(journeyId: String): Action[AnyContent] = auth.authoriseWithJourneySession { implicit request => vrn => journeySession =>

    def submitDirectDebitArrangement(sortCode: String, accountNumber: String, accountName: String) = {
      val directDebitArrangementRequest = DirectDebitArrangementRequest(
        journeySession.dayOfPayment.getOrElse(throw new RuntimeException(s"Day of payment is not set")),
        journeySession.numberOfPaymentMonths.getOrElse(throw new RuntimeException(s"Number of payment months not set")),
        journeySession.outStandingAmount.getOrElse(throw new RuntimeException(s"Outstanding amount is not set")),
        sortCode,
        accountNumber,
        accountName
      )
//       vatDeferralNewPaymentSchemeConnector.createDirectDebitArrangement(vrn.vrn, directDebitArrangementRequest).map {
//         case foo if foo.status == INTERNAL_SERVER_ERROR => Redirect(routes.PostCodeController.get())
//       }
//



//      try{
        vatDeferralNewPaymentSchemeConnector.createDirectDebitArrangement(vrn.vrn, directDebitArrangementRequest)
        Redirect(routes.PostCodeController.get())
//      } catch{
//        case _ => Redirect(routes.PostCodeController.get())
//      }


    }

    connector.complete(journeyId).map {
      case Some(r: PersonalCompleteResponse) => submitDirectDebitArrangement(r.sortCode, r.accountNumber, r.accountName)
      case Some(r: BusinessCompleteResponse) => submitDirectDebitArrangement(r.sortCode, r.accountNumber, r.companyName)
      case None => InternalServerError
    }
  }
}