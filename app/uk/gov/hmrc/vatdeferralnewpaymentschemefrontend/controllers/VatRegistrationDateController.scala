/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.{ EnterVatRegistrationDatePage, VatDetailsNotValidPage }
import scala.concurrent.Future
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import play.api.i18n.I18nSupport
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.http.{HttpResponse, NotFoundException}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.{KnownFactsSession, RequestSession, RootInterface, KnownFacts }
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth.Auth
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors.EnrolmentStoreConnector

@Singleton
class VatRegistrationDateController @Inject()(
  mcc: MessagesControllerComponents,
  auth: Auth,
  enrolmentStoreConnector: EnrolmentStoreConnector,
  enterVatRegistrationDatePage: EnterVatRegistrationDatePage,
  vatDetailsNotValidPage: VatDetailsNotValidPage)
  (implicit val appConfig: AppConfig, val serviceConfig: ServicesConfig)
    extends FrontendController(mcc) with I18nSupport {

  def get(): Action[AnyContent] = auth.authoriseForMatchingVrn { implicit request =>
    Future.successful(Ok(enterVatRegistrationDatePage()))
  }

  def post(): Action[AnyContent] = auth.authoriseForMatchingVrn { implicit request =>

    val form = request.body.asFormUrlEncoded.map { m =>
      m.mapValues(_.last)
    }.flatMap(parseFromMap)

    def renderView(date: String) = {
      RequestSession.getObject(request.session) match {

        case Some(knownFactsSession) => {

          val kf = Seq[KnownFacts] (
            KnownFacts("VRN", knownFactsSession.vrn),
            KnownFacts("Postcode", knownFactsSession.postCode.getOrElse("")),
            KnownFacts("BoxFiveValue", knownFactsSession.lastestVatAmount.getOrElse("")),
            KnownFacts("LastMonthLatestStagger", knownFactsSession.latestAccountPeriodMonth.getOrElse("")),
            KnownFacts("VATRegistrationDate", date))

          val ri = RootInterface("HMRC-MTD-VAT", kf)

          enrolmentStoreConnector.checkEnrolments(ri).flatMap { httpResponse =>
            httpResponse.status match {
              case OK => Future.successful(Redirect(routes.EligibilityController.get())
                .withSession(request.session + ("knownFactsSession" -> KnownFactsSession.convertToJson(
                  KnownFactsSession(knownFactsSession.vrn, knownFactsSession.postCode, knownFactsSession.lastestVatAmount, knownFactsSession.latestAccountPeriodMonth, Some(date), true)))
              ))
              case 204 => Future.successful(Ok(vatDetailsNotValidPage()))
              case _ => Future.successful(Ok("Api Failed")) // TODO: Add content here
            }
          }
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