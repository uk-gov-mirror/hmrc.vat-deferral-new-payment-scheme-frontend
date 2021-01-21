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
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.models.directdebitarrangement.DirectDebitArrangementRequest
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.models.{Eligibility, FinancialData}

import scala.concurrent.ExecutionContext

class VatDeferralNewPaymentSchemeConnector @Inject()(http: HttpClient, servicesConfig: ServicesConfig)(implicit val appConfig: AppConfig) {

  lazy val serviceURL = servicesConfig.baseUrl("vat-deferral-new-payment-scheme-service")

  def eligibility(vrn: String)(implicit hc: HeaderCarrier, ec: ExecutionContext)= {
    val url = s"${serviceURL}/vat-deferral-new-payment-scheme/eligibility/$vrn"
    http.GET[Eligibility](url)
  }

  def financialData(vrn: String)(implicit hc: HeaderCarrier, ec: ExecutionContext)= {
    val url = s"${serviceURL}/vat-deferral-new-payment-scheme/financialData/$vrn"
    http.GET[FinancialData](url)
  }

  def createDirectDebitArrangement(vrn: String, directDebitArrangementRequest: DirectDebitArrangementRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext)= {
    val url = s"${serviceURL}/vat-deferral-new-payment-scheme/direct-debit-arrangement/$vrn"
    http.POST[DirectDebitArrangementRequest, HttpResponse](url, directDebitArrangementRequest)
  }
}