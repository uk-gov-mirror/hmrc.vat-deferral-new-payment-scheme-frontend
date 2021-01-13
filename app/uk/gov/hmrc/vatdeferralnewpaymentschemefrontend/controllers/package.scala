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

package object controllers {

  def paymentStartDate: ZonedDateTime = {
    val now = ZonedDateTime.now.withZoneSameInstant(ZoneId.of("Europe/London"))
    val serviceStart: ZonedDateTime =
      ZonedDateTime.of(
        LocalDateTime.of(2021,2,15,1,1,1),
        ZoneId.of("Europe/London")
      )
    val today = if (now.isAfter(serviceStart)) now else serviceStart
    today match {
      case d if d.getDayOfMonth >= 15 && d.getDayOfMonth <= 22 && d.getMonthValue == 2 =>
        d.withDayOfMonth(3).withMonth(3)
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

}
