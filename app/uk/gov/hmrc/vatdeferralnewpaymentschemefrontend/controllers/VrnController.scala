/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.EnterVrnPage
import scala.concurrent.Future
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import play.api.i18n.I18nSupport
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.http.{HttpResponse, NotFoundException}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.KnownFactsSession

@Singleton
class VrnController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  http: HttpClient,
  enterVrnPage: EnterVrnPage)
  (implicit val appConfig: AppConfig, val serviceConfig: ServicesConfig)
    extends FrontendController(mcc) with I18nSupport {

  def get(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(enterVrnPage()))
  }

  def post(): Action[AnyContent] = Action.async { implicit request =>

    def renderView(vrn: String) = {

      if (vrn.equals("")) {
        Future.successful(Ok(enterVrnPage(vrn, Some("enter.vrn.required"))))
      }
      else if (isValid(vrn)) {
        Future.successful(Redirect(routes.PostCodeController.get())
          .withSession(request.session + ("knownFactsSession" -> KnownFactsSession.convertToJson(KnownFactsSession(vrn)))))
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