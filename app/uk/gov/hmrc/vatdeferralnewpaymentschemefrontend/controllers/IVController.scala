/*
 * Copyright 2020 HM Revenue & Customs
 *
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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class IVController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  ivConnector: IvConnector)
  (implicit val appConfig: AppConfig, val serviceConfig: ServicesConfig)
    extends FrontendController(mcc) with I18nSupport {

  def get(journeyId: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    journeyId match {
      case Some(id) =>
        ivConnector.getJourneyStatus(JourneyId(id)).flatMap {
          case Some(Success) =>
            Future.successful(Redirect(routes.EligibilityController.get()))
          case _ =>
            Future.successful(Ok("IV Failed"))
        }
      case None =>
        Future.successful(Ok("No Journey id"))
    }
  }
}