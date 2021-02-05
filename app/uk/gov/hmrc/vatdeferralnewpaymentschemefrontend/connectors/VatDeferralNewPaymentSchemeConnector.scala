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

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors

import com.google.inject.Inject
import play.api.Logger
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.controllers.audit
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.directdebitarrangement.{DirectDebitArrangementRequest, DirectDebitArrangementRequestAuditWrapper}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.{Eligibility, FinancialData}

import scala.concurrent.{ExecutionContext, Future}

class VatDeferralNewPaymentSchemeConnector @Inject()(
  http: HttpClient,
  servicesConfig: ServicesConfig
)(
  implicit val appConfig: AppConfig,
  auditConnector: AuditConnector
) {

  val logger = Logger(this.getClass)

  lazy val serviceURL: String = servicesConfig.baseUrl("vat-deferral-new-payment-scheme-service")

  def eligibility(vrn: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Eligibility] = {
    val url = s"$serviceURL/vat-deferral-new-payment-scheme/eligibility/$vrn"
    http.GET[Eligibility](url)
  }

  def financialData(vrn: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[FinancialData] = {
    val url = s"$serviceURL/vat-deferral-new-payment-scheme/financialData/$vrn"
    http.GET[FinancialData](url)
  }

  def createDirectDebitArrangement(
    vrn: String,
    directDebitArrangementRequest: DirectDebitArrangementRequest
  )(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[HttpResponse] = {
    val url = s"$serviceURL/vat-deferral-new-payment-scheme/direct-debit-arrangement/$vrn"
    http.POST[DirectDebitArrangementRequest, HttpResponse](url, directDebitArrangementRequest).recover {
      case e@UpstreamErrorResponse(message, 406, _, _ ) =>
        logger.error(message)
        audit(
          "DirectDebitSetup",
          DirectDebitArrangementRequestAuditWrapper(success = false, vrn, directDebitArrangementRequest)
        )
        throw e
    }
  }
}