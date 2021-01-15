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

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model

import play.api.libs.json.Json
import play.api.mvc

case class JourneySession (
  id: String,
  eligible: Boolean = false,
  outStandingAmount: Option[BigDecimal] = None,
  numberOfPaymentMonths: Option[Int] = None,
  dayOfPayment: Option[Int] = None
) {
  def monthsQuestion: Option[Boolean] = numberOfPaymentMonths match {
    case Some(11) => Some(true)
    case Some(_) => Some(false)
    case _ => None
  }
}

object JourneySession {
  implicit val formats = Json.format[JourneySession]
}