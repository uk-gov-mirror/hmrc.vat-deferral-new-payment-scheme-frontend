/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth

import com.google.inject.{ImplementedBy, Inject, Singleton}
import controllers.routes
import play.api.{Configuration, Environment, Logger, Mode}
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Name, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.{AuthProviders, ConfidenceLevel}
import java.net.{URLDecoder, URLEncoder}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AuthImpl])
trait Auth extends AuthorisedFunctions with AuthRedirects with Results {
  def authorise(action: Request[AnyContent] ⇒ Future[Result])(implicit ec: ExecutionContext, servicesConfig: ServicesConfig): Action[AnyContent]
}

@Singleton
class AuthImpl @Inject()(val authConnector: AuthConnector, val env: Environment, val config: Configuration, val appConfig: AppConfig) extends Auth {

  def authorise(action: Request[AnyContent] ⇒ Future[Result])(implicit ec: ExecutionContext, servicesConfig: ServicesConfig): Action[AnyContent] =
    Action.async { implicit request ⇒

      val currentUrl = {
        if (env.mode.equals(Mode.Dev)) s"http://${request.host}${request.uri}" else s"${request.uri}"
      }

      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSessionAndRequest(request.headers, Some(request.session), Some(request))

      // ConfidenceLevel.L200 and
      // Enrolment("HMCE-VATDEC-ORG") and
      authorised(AuthProviders(GovernmentGateway) and ConfidenceLevel.L200).retrieve(Retrievals.allEnrolments) {
        credentialRole => {
          action(request)
        }
      }.recover {
        case _: NoActiveSession => toGGLogin(currentUrl)
        case _: InsufficientConfidenceLevel | _: InsufficientEnrolments ⇒ SeeOther(appConfig.ivUrl(currentUrl))
        case ex => Ok("Error")
      }
    }
}