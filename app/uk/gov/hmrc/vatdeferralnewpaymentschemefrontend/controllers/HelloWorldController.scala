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
import play.api.i18n.I18nSupport
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.http.{HttpResponse, NotFoundException}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.Vrn
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth

@Singleton
class HelloWorldController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  helloWorldPage: HelloWorldPage)
  (implicit val appConfig: AppConfig, val serviceConfig: ServicesConfig)
    extends FrontendController(mcc) with I18nSupport {

  val helloWorld: Action[AnyContent] = auth.authorise { implicit request => implicit vrn =>
    Future.successful(Ok(helloWorldPage()))
  }

}
