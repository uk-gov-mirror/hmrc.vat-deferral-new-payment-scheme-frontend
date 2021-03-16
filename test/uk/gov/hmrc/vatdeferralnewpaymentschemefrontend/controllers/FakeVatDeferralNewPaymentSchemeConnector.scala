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

import java.time.{LocalDate, LocalDateTime, ZoneId, ZonedDateTime}

import play.api.http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors.VatDeferralNewPaymentSchemeConnector
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.{Eligibility, FinancialData, Vrn}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.directdebitarrangement.{DirectDebitArrangementRequest, InstallmentsAvailable}

import scala.concurrent.{ExecutionContext, Future}

class FakeVatDeferralNewPaymentSchemeConnector(
  testVrn: String,
  testFinancialData: FinancialData = FinancialData(Some(200000.00), 200000.00),
  testFirstPaymentDate: ZonedDateTime = ZonedDateTime.of(LocalDateTime.parse(LocalDate.parse("2021-03-15") + "T10:00:00"), ZoneId.of("Europe/London")),
  testCanPay: Boolean =  true,
  testInstallmentPeriodsAvailable: InstallmentsAvailable = InstallmentsAvailable(1,11)
) extends VatDeferralNewPaymentSchemeConnector {

  override def eligibility(
    vrn: String
  )(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Eligibility] = testVrn match {
    case "1000000000" => Future.successful(Eligibility(None,None,None,Some(false),Some(true)))
    case "1000000001" => Future.successful(Eligibility(Some(true),None,None,None,None))
    case "1000000002" => Future.successful(Eligibility(None,Some(true),None,None,None))
    case "1000000003" => Future.successful(Eligibility(None,None,Some(true),None,None))
    case "1000000004" => Future.successful(Eligibility(None,None,None,Some(true),None))
    case "1000000005" => Future.successful(Eligibility(None,None,None,None,None))
    case _ => ???
  }

  override def financialData(
    vrn: String
  )(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[FinancialData] = Future.successful(testFinancialData)

  override def createDirectDebitArrangement(
    vrn: String,
    directDebitArrangementRequest: DirectDebitArrangementRequest
  )(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[UpstreamErrorResponse,HttpResponse]] = testVrn match {
    case "9999999999" => Future.successful(Left(UpstreamErrorResponse("foo", 406)))
    case _ => Future.successful(Right(HttpResponse(Status.CREATED, "")))
  }

  override def firstPaymentDate(
    vrn: Vrn
  )(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[ZonedDateTime] = Future.successful(testFirstPaymentDate)

  override def canPay(
    vrn: Vrn,
    amount: BigDecimal
  )(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Boolean] = Future.successful(testCanPay)

  override def installmentPeriodsAvailable(
    vrn: Vrn,
    amount: BigDecimal
  )(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[InstallmentsAvailable] = Future.successful(testInstallmentPeriodsAvailable)
}
