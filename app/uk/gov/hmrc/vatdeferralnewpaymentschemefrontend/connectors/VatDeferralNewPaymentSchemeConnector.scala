/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors

import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.mvc.Http.Status.OK
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.models.iv._
import uk.gov.hmrc.http.{HttpResponse, NotFoundException}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.Eligibility
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig


import scala.concurrent.{ExecutionContext, Future}

class VatDeferralNewPaymentSchemeConnector @Inject()(http: HttpClient, servicesConfig: ServicesConfig)(implicit val appConfig: AppConfig) {

  lazy val serviceURL = servicesConfig.baseUrl("vat-deferral-new-payment-scheme-service")

  def eligibility(vrn: String)(implicit hc: HeaderCarrier, ec: ExecutionContext)= {
    val url = s"${serviceURL}/vat-deferral-new-payment-scheme/eligibility/$vrn"
    http.GET[Eligibility](url)
  }
}