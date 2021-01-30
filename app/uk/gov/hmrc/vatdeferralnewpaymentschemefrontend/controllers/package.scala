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
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.{DateFormValues, MatchingJourneySession}
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.enrolments.{EnrolmentRequest, EnrolmentResponse, Identifiers, KnownFacts}

import scala.concurrent.ExecutionContext
import scala.math.BigDecimal.RoundingMode

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
        case 1 if day != 11 => "st"
        case 2 if day != 12 => "nd"
        case 3 if day != 13 => "rd"
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

  object enrolments {

    private val HmrcMtdVatService = "HMRC-MTD-VAT"
    private val HmceVatdecOrgService = "HMCE-VATDEC-ORG"

    def enrolmentRequestHmrcMtdVat(matchingJourneySession: MatchingJourneySession) =
      EnrolmentRequest(
        HmrcMtdVatService,
        Seq[KnownFacts](
          KnownFacts("VRN", matchingJourneySession.vrn.getOrElse("")),
          KnownFacts("Postcode", matchingJourneySession.postCode.getOrElse("")))
      )

    def enrolmentRequestHmceVatdecOrg(matchingJourneySession: MatchingJourneySession) =
      EnrolmentRequest(
        HmceVatdecOrgService,
        Seq[KnownFacts](
          KnownFacts("VATRegNo", matchingJourneySession.vrn.getOrElse("")),
          KnownFacts("IRPCODE", matchingJourneySession.postCode.getOrElse("")))
      )

    def enrolmentMatches(
      enrolmentResponse: Option[EnrolmentResponse],
      journeyState: MatchingJourneySession
    ):Boolean = {
      enrolmentResponse match {
        case Some(er) if er.enrolments.isEmpty =>
          false
        case Some(er) =>
          er.enrolments.forall(
            enrolment => enrolment.verifiers.forall(
              identifiers => checkEnrolments(er.service, identifiers, journeyState)
            )
          )
        case _ =>
          false
      }
    }

    private def checkVatRegistrationDate(
      identifierValue: String,
      stateValue: Option[DateFormValues]
    ): Boolean = {
      identifierValue == formatStateDate(stateValue).getOrElse("")
    }

    private def formatStateDate(date: Option[DateFormValues]): Option[String] =
      date.map(dt => s"${"%02d".format(dt.day.toInt)}/${"%02d".format(dt.month.toInt)}/${dt.year.takeRight(2)}")

    private def formatLastAccountPeriodMonth(month: Option[String]) =
      month.fold("") { x =>
        LocalDate.now.withMonth(
          Integer.parseInt(x)
        ).format(
          DateTimeFormatter.ofPattern("MMM")
        ).toLowerCase()
      }

    private def checkEnrolments(
      service: String,
      identifiers: Identifiers,
      mjs: MatchingJourneySession
    ): Boolean = (service, identifiers.key, identifiers.value) match {
      case (HmrcMtdVatService, "BoxFiveValue", v) => v == mjs.latestVatAmount.getOrElse("")
      case (HmrcMtdVatService, "LastMonthLatestStagger", v) => v == mjs.latestAccountPeriodMonth.getOrElse("")
      case (HmrcMtdVatService, "VATRegistrationDate", v) => checkVatRegistrationDate(v, mjs.date)
      case (HmceVatdecOrgService, "PETAXDUESALES", v) => v == mjs.latestVatAmount.getOrElse("")
      case (HmceVatdecOrgService, "PEPDNO", v) => v == formatLastAccountPeriodMonth(mjs.latestAccountPeriodMonth)
      case (HmceVatdecOrgService, "IREFFREGDATE", v) => checkVatRegistrationDate(v, mjs.date)
      case _ => true
    }
  }
}
