/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model

import play.api.libs.json.Json

case class Eligibility(paymentPlanExists: Boolean, existingObligations: Boolean, outstandingBalance: Boolean)

object Eligibility {
  implicit val format = Json.format[Eligibility]
}