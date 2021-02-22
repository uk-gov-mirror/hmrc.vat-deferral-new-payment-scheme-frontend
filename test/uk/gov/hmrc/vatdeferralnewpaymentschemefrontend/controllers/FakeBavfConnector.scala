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

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.controllers

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.connectors.BavfConnector
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.Bavf._

import scala.concurrent.{ExecutionContext, Future}

class FakeBavfConnector extends BavfConnector {

  override def init(
    continueUrl: String,
    messages: Option[InitRequestMessages],
    customisationsUrl: Option[String],
    prepopulatedData: Option[InitRequestPrepopulatedData]
  )(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[InitResponse] = ???

  override def complete(
    journeyId: String,
    vrn: String
  )(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier): Future[Account] = {

    Future.successful(
      BusinessCompleteResponse(
        "foo", "123123", "123123", None
      )
    )
  }
}
