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

import play.api.http.Status
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors.VatDeferralNewPaymentSchemeConnector
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model._
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.directdebitarrangement.DirectDebitArrangementRequest

import scala.concurrent.{ExecutionContext, Future}

class EligibilityControllerSpec extends BaseSpec {

  "GET /" should {
    "return CheckBeforeYouStartPage" in {
      val controller = testController(new MockVatDeferralNewPaymentSchemeConnector("1000000000"))
      val result = controller.get(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe "/pay-vat-deferred-due-to-coronavirus/check-before-you-start"
    }

    "return ReturningUserPage" in {
      val controller = testController(new MockVatDeferralNewPaymentSchemeConnector("1000000001"))
      val result = controller.get(fakeRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include("returning.user.heading")
    }

    "return PaymentOnAccountExistsPage" in {
      val controller = testController(new MockVatDeferralNewPaymentSchemeConnector("1000000002"))
      val result = controller.get(fakeRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include("payment-on-account.heading")
    }

    "return TimeToPayExistsPage" in {
      val controller = testController(new MockVatDeferralNewPaymentSchemeConnector("1000000003"))
      val result = controller.get(fakeRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include("time-to-pay.heading")
    }

    "return OutstandingReturnsPage" in {
      val controller = testController(new MockVatDeferralNewPaymentSchemeConnector("1000000004"))
      val result = controller.get(fakeRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include("outstanding-returns.heading")
    }

    "return NoDeferredVatToPayPage" in {
      val controller = testController(new MockVatDeferralNewPaymentSchemeConnector("1000000005"))
      val result = controller.get(fakeRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include("no-vat-to-pay.heading")
    }
  }

  def testController(connector: VatDeferralNewPaymentSchemeConnector): EligibilityController = {
    new EligibilityController(
      auth,
      connector,
      sessionStore,
      notEligiblePage,
      returningUserPage,
      noDeferredVatToPayPage,
      timeToPayExistsPage,
      paymentOnAccountExistsPage,
      outstandingReturnsPage
    )
  }

  class MockVatDeferralNewPaymentSchemeConnector(testVrn: String) extends VatDeferralNewPaymentSchemeConnector {

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


}
