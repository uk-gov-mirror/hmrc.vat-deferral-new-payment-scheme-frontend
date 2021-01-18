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

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors

import com.google.inject.Inject
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsValue, Json}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.directdebitarrangement.DirectDebitArrangementRequest
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.{Eligibility, FinancialData}

import scala.concurrent.{ExecutionContext, Future}

class VatDeferralNewPaymentSchemeConnector @Inject()(http: HttpClient, servicesConfig: ServicesConfig)(implicit val appConfig: AppConfig) {

  lazy val serviceURL = servicesConfig.baseUrl("vat-deferral-new-payment-scheme-service")

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def eligibility(vrn: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Eligibility] = {
    val url = s"${serviceURL}/vat-deferral-new-payment-scheme/eligibility/$vrn"
    http.GET[Eligibility](url)
  }

  def financialData(vrn: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[FinancialData] = {
    val url = s"${serviceURL}/vat-deferral-new-payment-scheme/financialData/$vrn"
    http.GET[FinancialData](url)
  }

  def createDirectDebitArrangement(
    vrn: String,
    directDebitArrangementRequest: DirectDebitArrangementRequest
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val url = s"${serviceURL}/vat-deferral-new-payment-scheme/direct-debit-arrangement/$vrn"
    http.POST[DirectDebitArrangementRequest, HttpResponse](url, directDebitArrangementRequest).map {
      Result => {
        logger.info(s"Result of Direct Debit submission is $Result")
        Result
      }
    } recover {
      case upstreamErrorResponse: UpstreamErrorResponse => upstreamErrorResponse match {
        case Upstream5xxResponse(message, status, _, _) =>
          logger.error(s"[CitizenDetailsConnector][getDesignatoryDetails] - $message AAAAA")
          throw new Exception(s" ${status} return from FileUpload. ${message}")
      }
      case ex: Exception =>
        logger.error(s"[CitizenDetailsConnector][getDesignatoryDetails] - Designatory details returned an exception for - ${ex.getMessage}")
        throw ex
    }
  }
//  private def handleError(e: HttpException, vrn: String, directDebitArrangementRequest: DirectDebitArrangementRequest) = {
//    logger.error(s"Tax enrolment returned $e for $vrn with request of $directDebitArrangementRequest")
//    Upstream5xxResponse(e.message, e.responseCode, e.responseCode)
//  }


//  def subscribe(safeId: String, formBundleNumber: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
//      http.PUT[JsValue, HttpResponse](subscribeUrl(formBundleNumber), requestBody(safeId, formBundleNumber)) map {
//        Result => {
//          Logger.debug(
//            s"Tax Enrolments response is $Result"
//          )
//          Result
//        }
//      } recover {
//        case e: UnauthorizedException => handleError(e, formBundleNumber)
//        case e: BadRequestException => handleError(e, formBundleNumber)
//      }
//  }


}