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
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.i18n.MessagesApi
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.models.JourneySession
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.services.SessionStore
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.viewmodel.Month
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.{HowManyMonthsPage, MonthlyInstallmentsPage}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.math.BigDecimal.RoundingMode

@Singleton
class MonthsController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  monthlyInstallmentsPage: MonthlyInstallmentsPage,
  howManyMonthsPage: HowManyMonthsPage,
  sessionStore: SessionStore)
  (implicit val appConfig: AppConfig, val serviceConfig: ServicesConfig)
    extends BaseController(mcc) {

  val get: Action[AnyContent] = auth.authoriseWithJourneySession { implicit request => vrn => journeySession =>
    displayInstalmentsPage(
      journeySession,
      journeySession.monthsQuestion.fold(
        instalmentsForm
      )(
        x => instalmentsForm.fill(FormValues(x.toString))
      )
    )
  }

  val post: Action[AnyContent] = auth.authoriseWithJourneySession { implicit request => vrn => journeySession =>

    instalmentsForm.bindFromRequest().fold(
      errors => displayInstalmentsPage(journeySession, errors),
      formValue => {
          if(formValue.value == "true") {
            sessionStore.store[JourneySession](
              journeySession.id,
              "JourneySession",
              journeySession.copy(numberOfPaymentMonths = Some(11))
            )
            Future.successful(Redirect(routes.WhenToPayController.get()))
          }
          else {
            if (journeySession.numberOfPaymentMonths.isEmpty || journeySession.numberOfPaymentMonths.contains(11)) {
              sessionStore.store[JourneySession](
                journeySession.id,
                "JourneySession",
                journeySession.copy(numberOfPaymentMonths = Some(-1))
              )
            }
            Future.successful(Redirect(routes.MonthsController.getInstallmentBreakdown()))
          }
      }
    )
  }

  def getInstallmentBreakdown: Action[AnyContent] = auth.authoriseWithJourneySession { implicit request =>vrn =>journeySession =>
    displayMonthsSelectionPage(journeySession, monthForm)
  }

  def postInstallmentBreakdown: Action[AnyContent] = auth.authoriseWithJourneySession { implicit request => vrn => journeySession =>

    monthForm.bindFromRequest().fold(
      errors => displayMonthsSelectionPage(journeySession, errors),
      form => {
          sessionStore.store[JourneySession](journeySession.id, "JourneySession", journeySession.copy(numberOfPaymentMonths = Some(form.value.toInt)))
          Future.successful(Redirect(routes.WhenToPayController.get()))
      }
    )
  }

  def getMonths(amount: BigDecimal): Seq[Month] = {
    (2 to 10).map {
      month => {
        val monthlyAmount = (amount / month).setScale(2, RoundingMode.DOWN)
        val remainder = amount - (monthlyAmount * month)
        Month(month.toString, monthlyAmount.toString, remainder.toString)
      }
    }
  }

  def displayInstalmentsPage(
    journeySession: JourneySession,
    form: Form[FormValues]
  )(
    implicit request: Request[_],
    messages: MessagesApi,
    appConfig: AppConfig
  ): Future[Result] = {
    journeySession.outStandingAmount match {
      case Some(outStandingAmount) => {
        val monthlyAmount = (outStandingAmount / 11).setScale(2, RoundingMode.DOWN)
        val remainder = outStandingAmount - (monthlyAmount * 11)
        if (form.hasErrors) Future.successful(BadRequest(monthlyInstallmentsPage(monthlyAmount, remainder, form)))
        else Future.successful(Ok(monthlyInstallmentsPage(monthlyAmount, remainder, form)))
      }
      case _ => Future.successful(Redirect(routes.DeferredVatBillController.get()))
    }
  }

  def displayMonthsSelectionPage(
    journeySession: JourneySession,
    form: Form[FormValues]
  )(
    implicit request: Request[_],
    messages: MessagesApi,
    appConfig: AppConfig
  ): Future[Result] = {
    journeySession.outStandingAmount match {
      case Some(outStandingAmount) => {
        if (form.hasErrors) {
          Future.successful(BadRequest(
            howManyMonthsPage(
              getMonths(outStandingAmount).reverse,
              form
            )
          ))
        } else {
          Future.successful(Ok(
            howManyMonthsPage(
              getMonths(outStandingAmount).reverse,
              journeySession.numberOfPaymentMonths
                .fold(monthForm)(x => monthForm.fill(FormValues(x.toString)))
          )))
        }
      }
      case _ => Future.successful(Redirect(routes.DeferredVatBillController.get()))
    }
  }

  lazy val instalmentsForm: Form[FormValues] = Form(
    mapping("11-months-to-pay" -> mandatory("11monthstopay"))(FormValues.apply)(FormValues.unapply))

  lazy val monthForm: Form[FormValues] = Form(
    mapping("how-many-months" -> mandatory("how-many-months"))(FormValues.apply)(FormValues.unapply))

  case class FormValues(value: String)
}