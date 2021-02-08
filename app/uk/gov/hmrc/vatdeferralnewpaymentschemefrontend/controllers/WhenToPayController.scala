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

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors.BavfConnector
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.JourneySession
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.services.SessionStore
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.WhenToPayPage

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhenToPayController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  whenToPagePage: WhenToPayPage,
  connector: BavfConnector,
  sessionStore: SessionStore
)(
  implicit val appConfig: AppConfig,
  val serviceConfig: ServicesConfig,
  ec: ExecutionContext
) extends BaseController(mcc) {

  val logger = Logger(getClass)

  def get: Action[AnyContent] = auth.authoriseWithJourneySession { implicit request =>
    vrn =>
      journeySession =>
        (journeySession.outStandingAmount,journeySession.dayOfPayment) match {
          case (Some(_),dop) =>
            Future.successful(
              Ok(
                whenToPagePage(
                  dop.fold(frm)(x => frm.fill(FormValues(x.toString))),
                  formattedPaymentsStartDate,
                  journeySession.numberOfPaymentMonths.getOrElse(11)
                )
              ))
          case _ =>
            logger.warn("outStandingAmount and dayOfPayment cannot be retrieved from journeySession")
            Future.successful(
              Redirect(
                routes.DeferredVatBillController.get()
              )
            )
        }
  }

  def post: Action[AnyContent] = auth.authoriseWithJourneySession { implicit request => vrn => journeySession =>

        frm.bindFromRequest().fold(
          errors => Future.successful(
            BadRequest(
              whenToPagePage(
                errors,
                formattedPaymentsStartDate,
                journeySession.numberOfPaymentMonths.getOrElse(11)
              )
            )
          ),
          form => {
            sessionStore.store[JourneySession](journeySession.id, "JourneySession", journeySession.copy(dayOfPayment = Some(form.value.toInt)))
            Future.successful(Redirect(routes.PaymentPlanController.get()))
          }
        )
  }

  val frm: Form[FormValues] = Form(
    mapping("day" -> mandatoryAndValid("day", "^(2[0-8]|[1][0-9]|[1-9])$"))(FormValues.apply)(FormValues.unapply))

  case class FormValues(value: String)
}