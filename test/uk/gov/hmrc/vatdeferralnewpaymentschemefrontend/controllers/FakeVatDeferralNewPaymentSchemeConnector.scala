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

import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors.VatDeferralNewPaymentSchemeConnector
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.{Eligibility, FinancialData}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.directdebitarrangement.DirectDebitArrangementRequest

import scala.concurrent.{ExecutionContext, Future}

class FakeVatDeferralNewPaymentSchemeConnector(testVrn: String) extends VatDeferralNewPaymentSchemeConnector {

  override def eligibility(vrn: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Eligibility] = testVrn match {
    case "1000000000" => Future.successful(Eligibility(None,None,None,Some(false),Some(true)))
    case "1000000001" => Future.successful(Eligibility(Some(true),None,None,None,None))
    case "1000000002" => Future.successful(Eligibility(None,Some(true),None,None,None))
    case "1000000003" => Future.successful(Eligibility(None,None,Some(true),None,None))
    case "1000000004" => Future.successful(Eligibility(None,None,None,Some(true),None))
    case "1000000005" => Future.successful(Eligibility(None,None,None,None,None))
    case _ => ???
  }

  override def financialData(vrn: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[FinancialData] = ???

  override def createDirectDebitArrangement(vrn: String, directDebitArrangementRequest: DirectDebitArrangementRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = ???
}
