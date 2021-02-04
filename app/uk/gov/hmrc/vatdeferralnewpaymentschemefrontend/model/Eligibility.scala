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

import play.api.libs.json.{JsValue, Json, Writes}

case class Eligibility(
  paymentPlanExists: Option[Boolean],
  paymentOnAccoutExists: Option[Boolean],
  timeToPayExists: Option[Boolean],
  existingObligations: Option[Boolean],
  outstandingBalance: Option[Boolean]
) {
  // TODO - this relies on outstandingBalance only being set when all others are false - consider enum
  def eligible: Boolean = this match {
    case Eligibility(_,_,_,Some(false),Some(true)) => true
    case _ => false
  }
}

object Eligibility {
  implicit val format = Json.format[Eligibility]

  val auditWrites = new Writes[Eligibility] {
    override def writes(e: Eligibility): JsValue = Json.obj(
      "isEligible" -> e.eligible,
            "reasons" -> e
    )
  }
}

