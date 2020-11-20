/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.EnterLatestVatReturnTotalPage
import scala.concurrent.Future
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import play.api.i18n.I18nSupport
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.http.{HttpResponse, NotFoundException}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.{KnownFactsSession, RequestSession }

@Singleton
class VatReturnController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  http: HttpClient,
  enterLatestVatReturnTotalPage: EnterLatestVatReturnTotalPage)
  (implicit val appConfig: AppConfig, val serviceConfig: ServicesConfig)
    extends FrontendController(mcc) with I18nSupport {

  def get(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(enterLatestVatReturnTotalPage()))
  }

  def post(): Action[AnyContent] = Action.async { implicit request =>

    val form = request.body.asFormUrlEncoded.map { m =>
      m.mapValues(_.last)
    }.flatMap(parseFromMap)


    def renderView(amount: String) = {
      RequestSession.getObject(request.session) match {

        case Some(knownFactsSession) => {
          Console.println(s"knownFactsSession $knownFactsSession" )

          Future.successful(Redirect(routes.VatPeriodController.get())
            .withSession(request.session + ("knownFactsSession" -> KnownFactsSession.convertToJson(
              KnownFactsSession(knownFactsSession.vrn, knownFactsSession.postCode, Some(amount))))
          ))
        }

        case None => Future.successful(Redirect(routes.VrnController.get()))
      }
    }

    form match {
      case Some(postCode) => renderView(postCode)
      case None => Future.successful(BadRequest("error occured"))
    }
  }

  private def parseFromMap(in: Map[String, String]): Option[String] = {
    for {amount <- in.get("vat-amount")}
      yield {
        amount
      }
  }
}
