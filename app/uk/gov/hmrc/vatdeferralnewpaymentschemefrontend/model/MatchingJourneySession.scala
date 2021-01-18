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

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import play.api.libs.json.Json

case class DateFormValues(day: String, month: String, year: String) {
  def isValidDate: Boolean = try{
    val dateText = s"${"%02d".format(day.toInt)}/${"%02d".format(month.toInt)}/$year"
    LocalDate.parse(dateText, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    true
  }
  catch {
    case _ => false
  }
}

case object DateFormValues {
  implicit val format = Json.format[DateFormValues]

}

case class MatchingJourneySession (
  id: String,
  vrn: Option[String] = None,
  postCode: Option[String] = None,
  latestVatAmount: Option[String] = None,
  latestAccountPeriodMonth: Option[String] = None,
  date: Option[DateFormValues] = None,
  isUserEnrolled: Boolean = false,
  failedMatchingAttempts: Int = 0)

object MatchingJourneySession {
  implicit val formats = Json.format[MatchingJourneySession]
}

