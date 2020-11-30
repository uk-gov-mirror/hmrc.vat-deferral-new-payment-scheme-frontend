/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.viewmodel
import play.api.libs.json.Json
import play.api.mvc

case class Month(month: String, amount: String, remainder: String)