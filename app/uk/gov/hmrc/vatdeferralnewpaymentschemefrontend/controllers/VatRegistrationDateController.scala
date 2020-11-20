/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.EnterVatRegistrationDatePage
import scala.concurrent.Future
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import play.api.i18n.I18nSupport
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.http.{HttpResponse, NotFoundException}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.{KnownFactsSession, RequestSession, RootInterface, KnownFacts }
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

@Singleton
class VatRegistrationDateController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  http: HttpClient,
  enterVatRegistrationDatePage: EnterVatRegistrationDatePage)
  (implicit val appConfig: AppConfig, val serviceConfig: ServicesConfig)
    extends FrontendController(mcc) with I18nSupport {

  def get(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(enterVatRegistrationDatePage()))
  }

  def post(): Action[AnyContent] = Action.async { implicit request =>

    val form = request.body.asFormUrlEncoded.map { m =>
      m.mapValues(_.last)
    }.flatMap(parseFromMap)

    def renderView(date: String) = {
      RequestSession.getObject(request.session) match {

        case Some(knownFactsSession) => {

          Console.println(s"knownFactsSession $knownFactsSession & $date")

          val kf = Seq[KnownFacts] (
            KnownFacts("VRN", knownFactsSession.vrn),
            KnownFacts("Postcode", knownFactsSession.postCode.get),
            KnownFacts("BoxFiveValue", knownFactsSession.lastestVatAmount.get),
            KnownFacts("LastMonthLatestStagger", knownFactsSession.latestAccountPeriodMonth.get),
            KnownFacts("VATRegistrationDate", date))

          val ri = RootInterface("HMRC-MTD-VAT", kf)

          val data = {
            for {
              kfa <- http.POST[RootInterface, HttpResponse]("http://localhost:9595/enrolment-store-proxy/enrolment-store/enrolments", ri)
            } yield {
              kfa
            }
          }

          data map {
            a => Redirect(routes.TermsAndConditionsController.get())
          }
//          Future.successful(Redirect(routes.TermsAndConditionsController.get())
//            .withSession(request.session + ("knownFactsSession" -> KnownFactsSession.convertToJson(
//              KnownFactsSession(knownFactsSession.vrn, knownFactsSession.postCode, knownFactsSession.lastestVatAmount, knownFactsSession.latestAccountPeriodMonth, Some(date)))))
        }
        case None => Future.successful(Redirect(routes.VrnController.get()))
      }
    }

    form match {
      case Some(date) => renderView(date)
      case None => Future.successful(BadRequest("error occured"))
    }
  }

  private def parseFromMap(in: Map[String, String]): Option[String] = {
    for {
      day <- in.get("day")
      month <- in.get("month")
      year <- in.get("year")
    }
      yield {
        s"$day/$month/$year"
      }
  }
}