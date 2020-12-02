/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth

import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.{Configuration, Environment, Mode}
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.{AuthProviders, ConfidenceLevel}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.{ Vrn, RequestSession }

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AuthImpl])
trait Auth extends AuthorisedFunctions with AuthRedirects with Results {
  def authorise(action: Request[AnyContent] => Vrn => Future[Result])(implicit ec: ExecutionContext, servicesConfig: ServicesConfig): Action[AnyContent]
  def authoriseForMatchingVrn(action: Request[AnyContent] => Future[Result])(implicit ec: ExecutionContext, servicesConfig: ServicesConfig): Action[AnyContent]
}

@Singleton
class AuthImpl @Inject()(val authConnector: AuthConnector, val env: Environment, val config: Configuration, val appConfig: AppConfig) extends Auth {

  def authorise(action: Request[AnyContent] => Vrn => Future[Result])(implicit ec: ExecutionContext, servicesConfig: ServicesConfig): Action[AnyContent] =
    Action.async { implicit request =>

      val currentUrl = {
        if (env.mode.equals(Mode.Dev)) s"http://${request.host}${request.uri}" else s"${request.uri}"
      }

      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSessionAndRequest(request.headers, Some(request.session), Some(request))

      authorised(AuthProviders(GovernmentGateway) and ConfidenceLevel.L200).retrieve(Retrievals.allEnrolments) {
        enrolments => {

          def getVrnFromEnrolment(serviceKey: String, enrolmentKey: String): Option[Vrn] =
            for {
              e <- enrolments.getEnrolment(serviceKey)
              i <- e.getIdentifier(enrolmentKey)
            } yield Vrn(i.value)

          def getVrnFromSession: Option[Vrn] = {
            val sessionVrn = RequestSession.getObject(request.session)
            sessionVrn match {
              case Some(sessionVrn) if sessionVrn.isUserEnrolled => Some(Vrn(sessionVrn.vrn))
              case _ => None
            }
          }

          (
            getVrnFromEnrolment("HMRC-MTD-VAT", "VRN") orElse
            getVrnFromEnrolment("HMCE-VATDEC-ORG", "VATRegNo") orElse
            getVrnFromSession
          ).fold(Future.successful(Redirect("enter-vrn")))(action(request))
        }
      }.recover {
        case _: NoActiveSession => toGGLogin(currentUrl)
        case _: InsufficientConfidenceLevel | _: InsufficientEnrolments => SeeOther(appConfig.ivUrl(currentUrl))
        case ex => Ok("Error")
      }
    }

  def authoriseForMatchingVrn(action: Request[AnyContent] => Future[Result])(implicit ec: ExecutionContext, servicesConfig: ServicesConfig): Action[AnyContent] =
    Action.async { implicit request =>

      val currentUrl = {
        if (env.mode.equals(Mode.Dev)) s"http://${request.host}${request.uri}" else s"${request.uri}"
      }

      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSessionAndRequest(request.headers, Some(request.session), Some(request))

      authorised(AuthProviders(GovernmentGateway) and ConfidenceLevel.L200).retrieve(Retrievals.allEnrolments) {
        _ => action(request)
      }.recover {
        case _: NoActiveSession => toGGLogin(currentUrl)
        case _: InsufficientConfidenceLevel | _: InsufficientEnrolments => SeeOther(appConfig.ivUrl(currentUrl))
        case ex => Ok("Error")
      }
    }
}