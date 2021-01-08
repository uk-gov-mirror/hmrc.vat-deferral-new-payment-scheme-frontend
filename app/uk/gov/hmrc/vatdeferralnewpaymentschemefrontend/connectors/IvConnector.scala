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

import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.mvc.Http.Status.OK
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.models.iv._

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[IvConnectorImpl])
trait IvConnector {
  def getJourneyStatus(journeyId: JourneyId)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[IvResponse]]
}

@Singleton
class IvConnectorImpl @Inject() (http: HttpClient)(implicit val appConfig: AppConfig)
    extends IvConnector {

  implicit def toFuture[A](a: A): Future[A] = Future.successful(a)

  override def getJourneyStatus(journeyId: JourneyId)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[IvResponse]] =
    http.GET(appConfig.ivJourneyResultUrl(journeyId))
      .flatMap {
        case r if r.status == OK =>
          val result = (r.json \ "result").as[String]
          IvSuccessResponse.fromString(result)
        case r =>
          Some(IvUnexpectedResponse(r))
      }
      .recoverWith {
        case e: Exception =>
          Some(IvErrorResponse(e))
      }
}