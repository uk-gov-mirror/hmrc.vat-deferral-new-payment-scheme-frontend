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
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.MatchingJourneySession
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.services.SessionStore
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.EnterVrnPage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class VrnController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  sessionStore: SessionStore,
  enterVrnPage: EnterVrnPage)
  (implicit val appConfig: AppConfig, val serviceConfig: ServicesConfig)
    extends FrontendController(mcc) with I18nSupport {

  def get(): Action[AnyContent] = auth.authoriseWithMatchingJourneySession { implicit request =>_ =>
    Future.successful(Ok(enterVrnPage()))
  }

  def post(): Action[AnyContent] = auth.authoriseWithMatchingJourneySession { implicit request => matchingJourneySession =>

    def renderView(vrn: String) = {

      if (vrn.equals("")) {
        Future.successful(Ok(enterVrnPage(vrn, Some("enter.vrn.required"))))
      }
      else if (isValid(vrn)) {
        sessionStore.store[MatchingJourneySession](matchingJourneySession.id, "MatchingJourneySession", matchingJourneySession.copy(vrn = Some(vrn)))
        Future.successful(Redirect(routes.PostCodeController.get()))
      }
      else {
        Future.successful(Ok(enterVrnPage(vrn, Some("enter.vrn.invalid"))))
      }
    }

    val form = request.body.asFormUrlEncoded.map { m =>
      m.mapValues(_.last)
    }.flatMap(parseFromMap)

    form match {
      case Some(vrn) => renderView(vrn)
      case None => Future.successful(BadRequest("error occured"))
    }
  }

  def isValid(vrn: String) = true

  private def parseFromMap(in: Map[String, String]): Option[String] = {
    for {vrn <- in.get("vrn")}
      yield {
        vrn
      }
  }
}