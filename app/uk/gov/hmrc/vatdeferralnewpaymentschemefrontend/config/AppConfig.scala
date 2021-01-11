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

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config

import java.net.URI
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import java.net.{URLDecoder, URLEncoder}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.models.iv.{ IvResponse, JourneyId }

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {
  val footerLinkItems: Seq[String] = config.getOptional[Seq[String]]("footerLinkItems").getOrElse(Seq())

  private def getUrlFor(service: String) = servicesConfig.getString(s"microservice.services.$service.url")

  val frontendUrl: String = getUrlFor("frontend")

  val ivUpliftUrl: String = s"${getUrlFor("identity-verification-uplift")}/uplift"

  def ivUrl(redirectOnLoginURL: String): String = {

    def encodedCallbackUrl(redirectOnLoginURL: String): String = URLEncoder.encode(s"$frontendUrl/iv/journey-result?continueURL==$redirectOnLoginURL")

    new URI(
      s"$ivUpliftUrl" +
        s"?origin=VRNPS" +
        s"&completionURL=${encodedCallbackUrl(redirectOnLoginURL)}" +
        s"&failureURL=${encodedCallbackUrl(redirectOnLoginURL)}" +
        "&confidenceLevel=200"
    ).toString
  }

  val ivJourneyResultUrl: String =
    s"${servicesConfig.baseUrl("identity-verification-journey-result")}/mdtp/journey/journeyId"

  def ivJourneyResultUrl(journeyId: JourneyId): String = new URI(s"$ivJourneyResultUrl/${journeyId.Id}").toString

  val enrolmentStoreUrl = s"${servicesConfig.baseUrl("enrolment-store-proxy")}/enrolment-store-proxy/enrolment-store/enrolments"

  lazy val feedbackSurveyUrl: String = servicesConfig.getConfString("feedback-survey.url", "")

  val contactHost: String = servicesConfig.baseUrl(s"contact-frontend")
  //TODO Check the service identifier for feedback
  lazy val betaFeedbackUrlNoAuth = s"$contactHost/contact/beta-feedback-unauthenticated?service=VDNPS"

  val bavfApiBaseUrl = servicesConfig.baseUrl("bank-account-verification-api")
  val bavfWebBaseUrl = servicesConfig.baseUrl("bank-account-verification-web")

  val vrnRegex = servicesConfig.getString(s"regex.vrn")
  val decimalRegex = servicesConfig.getString(s"regex.decimal")
  val postCodeRegex = servicesConfig.getString(s"regex.postCode")
}