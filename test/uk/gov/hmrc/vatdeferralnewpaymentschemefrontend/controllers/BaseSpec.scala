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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Lang
import play.api.test.FakeRequest
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.services.SessionStore
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html._
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.errors._
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.play.language.LanguageUtils

import scala.concurrent.ExecutionContext

abstract class BaseSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar {

  val environ                   = Environment.simple()
  val configuration             = Configuration.load(environ)
  implicit val serviceConfig    = new ServicesConfig(configuration)
  val fakeRequest               = FakeRequest("GET", "/").withSession(("sessionId","foo"))
  implicit val mcc              = uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents()
  implicit val lang: Lang       = Lang.defaultLang
  implicit val appConfig        = new AppConfig(configuration, serviceConfig)
  implicit val executionContext = app.injector.instanceOf[ExecutionContext]
  implicit val headerCarrier    = HeaderCarrier()
  implicit val auditConnector   = mock[AuditConnector]
  val messagesApi               = mcc.messagesApi
  val mockAuthConnector         = mock[AuthConnector]
  val auth: Auth                = new FakeAuth(mockAuthConnector,environ,configuration)
  val sessionStore              = mock[SessionStore]
  val languageUtils             = app.injector.instanceOf[LanguageUtils]

  val notEligiblePage: NotEligiblePage = app.injector.instanceOf[NotEligiblePage]
  val returningUserPage: ReturningUserPage = app.injector.instanceOf[ReturningUserPage]
  val noDeferredVatToPayPage: NoDeferredVatToPayPage = app.injector.instanceOf[NoDeferredVatToPayPage]
  val timeToPayExistsPage: TimeToPayExistsPage = app.injector.instanceOf[TimeToPayExistsPage]
  val paymentOnAccountExistsPage: PaymentOnAccountExistsPage = app.injector.instanceOf[PaymentOnAccountExistsPage]
  val outstandingReturnsPage: OutstandingReturnsPage = app.injector.instanceOf[OutstandingReturnsPage]
  val directDebitPage: DirectDebitPage = app.injector.instanceOf[DirectDebitPage]
  val ddFailurePage: DDFailurePage = app.injector.instanceOf[DDFailurePage]
  val deferredVatBillPage: DeferredVatBillPage = app.injector.instanceOf[DeferredVatBillPage]
  val howManyMonthsPage: HowManyMonthsPage = app.injector.instanceOf[HowManyMonthsPage]
  val monthlyInstallmentsPage: MonthlyInstallmentsPage = app.injector.instanceOf[MonthlyInstallmentsPage]

}
