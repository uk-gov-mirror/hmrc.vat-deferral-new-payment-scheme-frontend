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
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors.{BavfConnector, VatDeferralNewPaymentSchemeConnector}

class BankDetailsControllerSpec extends BaseSpec {

  "POST /post" should {
    "return DDFailurePage" in {
      val controller =
        testController(
          new FakeVatDeferralNewPaymentSchemeConnector("9999999999"),
          new FakeBavfConnector()
        )
      val result =
        controller
          .post("foo")
          .apply(FakeRequest("POST", "/bank-details/:journeyId"))
      status(result) shouldBe Status.OK
      contentAsString(result) should include("dd.failure.heading")
    }
    "return redirect (to confirmation page)" in {
      val controller =
        testController(
          new FakeVatDeferralNewPaymentSchemeConnector("100000000"),
          new FakeBavfConnector()
        )
      val result =
        controller
          .post("foo")
          .apply(FakeRequest("POST", "/bank-details/:journeyId"))
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) should be
      "/pay-vat-deferred-due-to-coronavirus/confirmation"
    }
  }

  def testController(
    connector: VatDeferralNewPaymentSchemeConnector,
    bavfConnector: BavfConnector
  ): BankDetailsController = {
    new BankDetailsController(
      mcc,
      auth,
      directDebitPage,
      ddFailurePage,
      bavfConnector,
      sessionStore,
      connector
    )
  }
}
