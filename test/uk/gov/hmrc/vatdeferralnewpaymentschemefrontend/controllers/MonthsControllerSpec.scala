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
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors.VatDeferralNewPaymentSchemeConnector
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.{JourneySession, Submission}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.directdebitarrangement.InstallmentsAvailable

import scala.concurrent.Future

class MonthsControllerSpec extends BaseSpec {

  "GET /installments " should {
    "Return displayInstallments page if 11 months are available" in {
      val controller = testController(
        new FakeVatDeferralNewPaymentSchemeConnector(
          "9999999999"
        )
      )
      val result = controller.get(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsString(result) should include("installments.h1")
    }

    "Redirect to howManyMonthsPage when there is less than 11 months available" in {
      val controller = testController(
        new FakeVatDeferralNewPaymentSchemeConnector(
          "9999999999",
          testInstallmentPeriodsAvailable = InstallmentsAvailable(1, 8)
        )
      )
      val result = controller.get(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe "/pay-vat-deferred-due-to-coronavirus/installments-breakdown"
    }

  }

  "GET /installments-breakdown" should {
    "Display howManyMonthsPage with 5 radio buttons for 5 InstallmentsAvailable" in {
      val controller = testController(
        new FakeVatDeferralNewPaymentSchemeConnector(
          "9999999999",
          testInstallmentPeriodsAvailable = InstallmentsAvailable(1, 5)
        )
      )
      when(sessionStore.get[JourneySession](any(), any())(any())).thenReturn(
        Future.successful(Option(JourneySession("foo", true, Some(BigDecimal(100.00)), Some(5), Some(1), Submission(false))))
      )
      val result = controller.getInstallmentBreakdown(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsString(result) should include("how.many.months.h1")
      contentAsString(result) should include("how-many-months-5")
      contentAsString(result) shouldNot include("how-many-months-6")
    }

    "Display howManyMonthsPage with 10 radio buttons for 10 InstallmentsAvailable" in {
      val controller = testController(
        new FakeVatDeferralNewPaymentSchemeConnector(
          "9999999999",
          testInstallmentPeriodsAvailable = InstallmentsAvailable(1, 10)
        )
      )
      when(sessionStore.get[JourneySession](any(), any())(any())).thenReturn(
        Future.successful(Option(JourneySession("foo", true, Some(BigDecimal(100.00)), Some(10), Some(1), Submission(false))))
      )
      val result = controller.getInstallmentBreakdown(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsString(result) should include("how.many.months.h1")
      contentAsString(result) should include("how-many-months-10")
      contentAsString(result) shouldNot include("how-many-months-11")
    }
  }

  def testController(
    connector: VatDeferralNewPaymentSchemeConnector
  ): MonthsController = {
    new MonthsController(
      mcc,
      auth,
      monthlyInstallmentsPage,
      howManyMonthsPage,
      sessionStore,
      languageUtils,
      connector
    )
  }

}
