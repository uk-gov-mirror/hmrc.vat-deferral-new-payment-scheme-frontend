/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model

import play.api.libs.json.Json

case class FinancialData(originalAmount: BigDecimal, outstandingAmount: BigDecimal)

object FinancialData {
  implicit val format = Json.format[FinancialData]
}