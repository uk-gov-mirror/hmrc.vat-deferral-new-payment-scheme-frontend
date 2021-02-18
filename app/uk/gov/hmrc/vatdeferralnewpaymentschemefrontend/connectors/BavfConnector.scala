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

import javax.inject.Inject
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.http._
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.Bavf._
import InitRequest.writes
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

class BavfConnector @Inject()(httpClient: HttpClient)(implicit val appConfig: AppConfig) {

  val logger = Logger(getClass)

  def init(
    continueUrl: String,
    messages: Option[InitRequestMessages] = None,
    customisationsUrl: Option[String] = None,
    prepopulatedData: Option[InitRequestPrepopulatedData] = None
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[InitResponse] = {

    val request = InitRequest(
      "vdnps",
      continueUrl,
      messages,
      customisationsUrl,
      address = None,
      prepopulatedData
    )

    val url = s"${appConfig.bavfApiBaseUrl}/api/init"
    httpClient.POST[InitRequest, InitResponse](url, request).recover {
      case e: UpstreamErrorResponse =>
        logger.warn(s"init connector failed to BAVFE with statusCode: ${e.statusCode} and message: ${e.message}")
        throw e
    }
  }

  def complete(journeyId: String)(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[Account] = {
    val url = s"${appConfig.bavfApiBaseUrl}/api/complete/$journeyId"
    httpClient.GET[Account](url).recover {
      case e: UpstreamErrorResponse =>
        logger.warn(s"JourneyId: $journeyId - complete connector failed from BAVFE with statusCode: ${e.statusCode} and message: ${e.message}")
      throw e
    }
  }
}


