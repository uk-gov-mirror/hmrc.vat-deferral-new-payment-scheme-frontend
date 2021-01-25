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

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend

import java.time._
import java.time.format.DateTimeFormatter

import play.api.i18n.Messages
import play.api.libs.json.Writes
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext
import scala.math.BigDecimal.RoundingMode

package object controllers {

  def paymentStartDate: ZonedDateTime = {
    val now = ZonedDateTime.now.withZoneSameInstant(ZoneId.of("Europe/London"))
    // TODO consider turning this into a config
    val serviceStart: ZonedDateTime =
      ZonedDateTime.of(
        LocalDateTime.of(2021,2,15,1,1,1),
        ZoneId.of("Europe/London")
      )
    val today = if (now.isAfter(serviceStart)) now else serviceStart
    today match {
      case d if d.getDayOfMonth >= 15 && d.getDayOfMonth <= 22 && d.getMonthValue == 2 =>
        d.withDayOfMonth(1).withMonth(3)
      case d if d.plusDays(5).getDayOfWeek.getValue <= 5 =>
        d.plusDays(5)
      case d if d.plusDays(5).getDayOfWeek.getValue == 6 =>
        d.plusDays(7)
      case d if d.plusDays(5).getDayOfWeek.getValue == 7 =>
        d.plusDays(6)
    }
  }

  def formattedPaymentsStartDate: String =
    paymentStartDate.format(DateTimeFormatter.ofPattern("d MMMM YYYY"))

  def firstPaymentAmount(amountOwed: BigDecimal, periodOwed: Int): BigDecimal = {
    val monthlyAmount = regularPaymentAmount(amountOwed, periodOwed)
    val remainder = amountOwed - (monthlyAmount * periodOwed)
    monthlyAmount + remainder
  }

  def regularPaymentAmount(amountOwed: BigDecimal, periodOwed: Int): BigDecimal = {
    (amountOwed / periodOwed).setScale(2, RoundingMode.DOWN)
  }

  def daySuffix(day: Int)(implicit messages: Messages): String = {
    if(messages.lang.code == "cy"){
      day.toString
    } else{
      day % 10 match {
        case 1 => "st"
        case 2 => "nd"
        case 3 => "rd"
        case _ => "th"
      }
    }
  }

  def audit[T](
    auditType: String,
    result: T
  )(
    implicit headerCarrier: HeaderCarrier,
    auditConnector: AuditConnector,
    ec: ExecutionContext,
    writes: Writes[T]
  ): Unit = {
    import play.api.libs.json.Json
    auditConnector.sendExplicitAudit(
      auditType,
      Json.toJson(result)(writes)
    )
  }
}
