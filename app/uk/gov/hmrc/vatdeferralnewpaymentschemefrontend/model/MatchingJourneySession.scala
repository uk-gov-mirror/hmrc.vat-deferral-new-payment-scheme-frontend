/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model

import play.api.libs.json.Json

case class MatchingJourneySession (
  id: String,
  vrn: Option[String] = None,
  postCode: Option[String] = None,
  latestVatAmount: Option[String] = None,
  latestAccountPeriodMonth: Option[String] = None,
  date: Option[String] = None,
  isUserEnrolled: Boolean = false)

object MatchingJourneySession {
  implicit val formats = Json.format[MatchingJourneySession]
}