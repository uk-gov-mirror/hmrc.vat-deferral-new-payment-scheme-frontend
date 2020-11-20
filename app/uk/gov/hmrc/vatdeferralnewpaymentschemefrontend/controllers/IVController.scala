/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.controllers


import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.HelloWorldPage
import scala.concurrent.Future
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import play.api.i18n.I18nSupport
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.http.{HttpResponse, NotFoundException}

@Singleton
class IVController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  http: HttpClient,
  helloWorldPage: HelloWorldPage)
  (implicit val appConfig: AppConfig, val serviceConfig: ServicesConfig)
    extends FrontendController(mcc) with I18nSupport {

  def get(journeyId: Option[String]): Action[AnyContent] = Action.async  { implicit request =>
    for {
      uplift <- http.GET[HttpResponse](s"http://localhost:9948/mdtp/journey/journeyId/${journeyId.getOrElse(0)}")
    } yield {
      Ok(s"enrolment: ${uplift.body}")
    }
  }
}