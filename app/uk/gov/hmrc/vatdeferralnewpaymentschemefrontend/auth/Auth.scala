/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.auth

import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.mvc._
import play.api.{Configuration, Environment, Mode}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthProviders, ConfidenceLevel, _}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.config.{AuthRedirects, ServicesConfig}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.controllers.routes
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.{JourneySession, MatchingJourneySession, Vrn}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.services.SessionStore

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AuthImpl])
trait Auth extends AuthorisedFunctions with AuthRedirects with Results {
  def authorise(action: Request[AnyContent] => Vrn => Future[Result])(implicit ec: ExecutionContext, servicesConfig: ServicesConfig): Action[AnyContent]
  def authoriseWithJourneySession(action: Request[AnyContent] => Vrn => JourneySession => Future[Result])(implicit ec: ExecutionContext, servicesConfig: ServicesConfig): Action[AnyContent]
  def authoriseForMatchingJourney(action: Request[AnyContent] => Future[Result])(implicit ec: ExecutionContext, servicesConfig: ServicesConfig): Action[AnyContent]
  def authoriseWithMatchingJourneySession(action: Request[AnyContent] => MatchingJourneySession => Future[Result])(implicit ec: ExecutionContext, servicesConfig: ServicesConfig): Action[AnyContent]
}

@Singleton
class AuthImpl @Inject()(
  val authConnector: AuthConnector,
  val env: Environment,
  val config: Configuration,
  val appConfig: AppConfig,
  sessionStore: SessionStore) extends Auth {

  def authorise(action: Request[AnyContent] => Vrn => Future[Result])(implicit ec: ExecutionContext, servicesConfig: ServicesConfig): Action[AnyContent] =
    Action.async { implicit request =>

      val currentUrl = {
        if (env.mode.equals(Mode.Dev)) s"http://${request.host}${request.uri}" else s"${request.uri}"
      }

      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSessionAndRequest(request.headers, Some(request.session), Some(request))

      authorised(AuthProviders(GovernmentGateway) and ConfidenceLevel.L50).retrieve(Retrievals.allEnrolments) {
        enrolments => {

          def getVrnFromEnrolment(serviceKey: String, enrolmentKey: String): Option[Vrn] =
            for {
              e <- enrolments.getEnrolment(serviceKey)
              i <- e.getIdentifier(enrolmentKey)
            } yield Vrn(i.value)

          def getVrnFromSession(): Future[Result] = request.session.get("sessionId") match {
            case Some(sessionId) =>
              sessionStore.get[MatchingJourneySession](sessionId, "MatchingJourneySession").flatMap {
                case Some(a) if a.vrn.isDefined && a.isUserEnrolled => action(request)(Vrn(a.vrn.getOrElse(throw new RuntimeException("Vrn not set"))))
                case _ => Future.successful(Redirect("enter-vrn"))
              }
            case None => Future.successful(Redirect("enter-vrn"))
          }

          (
            getVrnFromEnrolment("HMRC-MTD-VAT", "VRN") orElse
            getVrnFromEnrolment("HMCE-VATDEC-ORG", "VATRegNo")
          ).fold(getVrnFromSession())(action(request))
        }
      }.recover {
        case _: NoActiveSession => toGGLogin(currentUrl)
      }
    }

  def authoriseWithJourneySession(action: Request[AnyContent] => Vrn => JourneySession => Future[Result])(implicit ec: ExecutionContext, servicesConfig: ServicesConfig): Action[AnyContent] =
    Action.async { implicit request =>

      val currentUrl = {
        if (env.mode.equals(Mode.Dev)) s"http://${request.host}${request.uri}" else s"${request.uri}"
      }

      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSessionAndRequest(request.headers, Some(request.session), Some(request))

      authorised(AuthProviders(GovernmentGateway) and ConfidenceLevel.L50).retrieve(Retrievals.allEnrolments) {
        enrolments => {

          def getVrnFromEnrolment(serviceKey: String, enrolmentKey: String): Option[Vrn] =
            for {
              e <- enrolments.getEnrolment(serviceKey)
              i <- e.getIdentifier(enrolmentKey)
            } yield Vrn(i.value)

          def getVrnFromSession(): Future[Result] = request.session.get("sessionId") match {
            case Some(sessionId) =>
              sessionStore.get[MatchingJourneySession](sessionId, "MatchingJourneySession").flatMap {
                case Some(a) if a.vrn.isDefined && a.isUserEnrolled => withJourneySession(Vrn(a.vrn.get))
                case _ => Future.successful(Redirect("enter-vrn"))
              }
            case None => Future.successful(Redirect("enter-vrn"))
          }

          def withJourneySession(vrn: Vrn): Future[Result] = request.session.get("sessionId") match {
            case Some(sessionId) =>
              sessionStore.get[JourneySession](sessionId, "JourneySession").flatMap {
                case Some(a) if a.redirect(request).isDefined =>
                  a.redirect(request).getOrElse(
                    throw new IllegalStateException("unable to get defined option")
                  )
                case Some(a) => action(request)(vrn)(a)
                case _ => Future.successful(Redirect(routes.DeferredVatBillController.get()))
              }
            case None => Future.successful(toGGLogin(currentUrl))
          }

          (
            getVrnFromEnrolment("HMRC-MTD-VAT", "VRN") orElse
            getVrnFromEnrolment("HMCE-VATDEC-ORG", "VATRegNo")
          ).fold(getVrnFromSession)(vrn => withJourneySession(vrn))
        }
      }.recover {
        case _: NoActiveSession => toGGLogin(currentUrl)
      }
    }

  def authoriseForMatchingJourney(action: Request[AnyContent] => Future[Result])(implicit ec: ExecutionContext, servicesConfig: ServicesConfig): Action[AnyContent] =
    Action.async { implicit request =>

      val currentUrl = {
        if (env.mode.equals(Mode.Dev)) s"http://${request.host}${request.uri}" else s"${request.uri}"
      }

      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSessionAndRequest(request.headers, Some(request.session), Some(request))

      authorised(AuthProviders(GovernmentGateway) and ConfidenceLevel.L50).retrieve(Retrievals.allEnrolments) {
        _ => action(request)
      }.recover {
        case _: NoActiveSession => toGGLogin(currentUrl)
      }
    }

  def authoriseWithMatchingJourneySession(action: Request[AnyContent] => MatchingJourneySession => Future[Result])(implicit ec: ExecutionContext, servicesConfig: ServicesConfig): Action[AnyContent] =
    Action.async { implicit request =>

      val currentUrl = {
        if (env.mode.equals(Mode.Dev)) s"http://${request.host}${request.uri}" else s"${request.uri}"
      }

      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSessionAndRequest(request.headers, Some(request.session), Some(request))

      val newAction: Future[Result] =
        request.session.get("sessionId").map { sessionId =>
          sessionStore.get[MatchingJourneySession](sessionId, "MatchingJourneySession").flatMap {
            case Some(a) => {
              val redirect = a.redirect(request)
              if (redirect.isDefined)
                redirect.getOrElse(throw new IllegalStateException("unable to get defined option"))
              else if (!a.locked)
                action(request)(a)
              else
                Future.successful(Redirect(routes.NotMatchedController.get()))
            }
            case _ => {
              sessionStore.store[MatchingJourneySession](sessionId, "MatchingJourneySession", MatchingJourneySession(sessionId))
              action(request)(MatchingJourneySession(sessionId))
            }
          }
        }.getOrElse(Future.successful(toGGLogin(currentUrl)))

      authorised(AuthProviders(GovernmentGateway) and ConfidenceLevel.L50).retrieve(Retrievals.allEnrolments) {
        _ => newAction
      }.recover {
        case _: NoActiveSession => toGGLogin(currentUrl)
      }
    }
}