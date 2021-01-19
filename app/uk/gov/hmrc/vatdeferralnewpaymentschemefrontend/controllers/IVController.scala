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
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors.IvConnector
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.models.iv.IvSuccessResponse._
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.models.iv._
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.iv._
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.iv.technical_iv_issues

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class IVController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  ivConnector: IvConnector,
  failedIVview: failed_iv,
  insufficientEvidenceView: insufficient_evidence,
  userAbortedView: user_aborted,
  lockedOutView: locked_out,
  technicalIVissuesView: technical_iv_issues,
  timeOutView: time_out,
  preconditionFailedView: precondition_failed
)
  (implicit val appConfig: AppConfig, val serviceConfig: ServicesConfig)
    extends FrontendController(mcc) with I18nSupport {

  val retryUrl = appConfig.ivUrl(routes.EligibilityController.get().url)

  def get(journeyId: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    journeyId match {
      case Some(id) =>
        ivConnector.getJourneyStatus(JourneyId(id)).flatMap {
          case Some(Success) =>
            Future.successful(Redirect(routes.EligibilityController.get()))
          case Some(Incomplete) =>
            Future.successful(Ok(technicalIVissuesView(retryUrl)))
          case Some(FailedIV) =>
            Future.successful(Ok(failedIVview(retryUrl)))
          case Some(InsufficientEvidence) =>
            Future.successful(Ok(insufficientEvidenceView()))
          case Some(UserAborted) =>
            Future.successful(Ok(userAbortedView(retryUrl)))
          case Some(LockedOut) =>
            Future.successful(Ok(lockedOutView(retryUrl)))
          case Some(PrecondFailed) => // why does help to save send them to GG
            Future.successful(Ok(preconditionFailedView()))
          case Some(TechnicalIssue) =>
            Future.successful(Ok(technicalIVissuesView(retryUrl)))
          case Some(Timeout) =>
            Future.successful(Ok(timeOutView(retryUrl)))
            // TODO help-to-save don't handle FailedMatching
          case _ =>
            Future.successful(Ok(technicalIVissuesView(retryUrl)))

        }
      case None =>
        Future.successful(Ok("No Journey id"))
    }
  }
}