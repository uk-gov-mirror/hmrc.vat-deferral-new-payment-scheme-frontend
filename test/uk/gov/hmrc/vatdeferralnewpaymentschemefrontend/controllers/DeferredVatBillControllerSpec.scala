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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.test.Helpers._
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors.VatDeferralNewPaymentSchemeConnector
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.{JourneySession, Submission}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.services.SessionStore
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.DeferredVatBillPage

import scala.concurrent.Future

class DeferredVatBillControllerSpec extends BaseSpec {

  when(sessionStore.store[JourneySession](any(), any(), any())(any(),any())).thenReturn(
    Future.successful(JourneySession("foo", true, Some(BigDecimal(100.00)), Some(11), Some(1), Submission(false)))
  )

  "GET /deferred-vat-bill" should {
    "tell you your deferred-vat-bill and contain continue button" in {
      val controller = testController(
        new FakeVatDeferralNewPaymentSchemeConnector(
          "9999999999",
          testCanPay = true
        )
      )
      val result = controller.get(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsString(result) should include("global.button.continue")
    }

    "tell you your outstanding balance is too large for the £20m DD limit to use the service " +
      "and contain log out button" in {
      val controller = testController(
        new FakeVatDeferralNewPaymentSchemeConnector(
          "9999999999",
          testCanPay = false
        )
      )
      val result = controller.get(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsString(result) should include("finish.sign-out.button")
      contentAsString(result) should include("deferred.vat.bill.toobig.p1")
    }
  }

  def testController(
    connector: VatDeferralNewPaymentSchemeConnector
  ):DeferredVatBillController = {
    new DeferredVatBillController(
      mcc,
      auth: Auth,
      connector,
      deferredVatBillPage: DeferredVatBillPage,
      sessionStore: SessionStore

    )
  }

}
