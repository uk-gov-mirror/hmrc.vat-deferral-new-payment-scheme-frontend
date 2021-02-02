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

import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Request
import shapeless.syntax.std.tuple._
import shapeless.syntax.typeable._
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.journey.Uri

case class JourneySession (
  id: String,
  eligible: Boolean = false,
  outStandingAmount: FormPage[BigDecimal] = FormPage("deferred-vat-bill"),
  numberOfPaymentMonths: FormPage[Int] = FormPage("installments-breakdown"),
  dayOfPayment: FormPage[Int] = FormPage("when-to-pay")
) extends Journey {

  def monthsQuestion: FormPage[Boolean] = numberOfPaymentMonths.value match {
    case Some(11) => FormPage[Boolean]("installments", Some(true))
    case Some(_) => FormPage[Boolean]("installments", Some(false))
    case None => FormPage[Boolean]("installments")
  }

  override def steps: List[Option[FormPage[_]]] = {
    JourneySession
      .unapply(this)
      .map(_.toList)
      .fold(List.empty[Any])(identity)
      .map(_.cast[FormPage[_]])
  }

  override val formPages: List[FormPage[_]] = {
    val pages = steps.filter(_.nonEmpty).flatten
    pages.head :: monthsQuestion :: pages.tail
  }

  override def isEmptyFormPage(
    indexedFormPage: (FormPage[_],Int)
  )(
    implicit request: Request[_]
  ): Boolean = indexedFormPage match {
    case (formPage, index) =>
      (formPage.value.isEmpty || formPage.value.contains(-1)) && index < currentIndex
  }

}

object JourneySession {
  implicit val formPageBigDecimalFormat: OFormat[FormPage[BigDecimal]] =
    Json.format[FormPage[BigDecimal]]
  implicit val formPageBooleanValueFormat: OFormat[FormPage[Boolean]] =
    Json.format[FormPage[Boolean]]
  implicit val formPageIntFormat: OFormat[FormPage[Int]] =
    Json.format[FormPage[Int]]
  implicit val formats: OFormat[JourneySession] =
    Json.format[JourneySession]
}


//case class JourneySession (
//  id: String,
//  eligible: Boolean = false,
//  outStandingAmount: Option[BigDecimal] = None,
//  numberOfPaymentMonths: Option[Int] = None,
//  dayOfPayment: Option[Int] = None
//) {
//  def monthsQuestion: Option[Boolean] = numberOfPaymentMonths match {
//    case Some(11) => Some(true)
//    case Some(_) => Some(false)
//    case _ => None
//  }
//
//  def redirect(request: Request[AnyContent]): Option[Future[Result]] = {
//
//    val formPages: List[Option[_]] = {
//      val js = JourneySession
//        .unapply(this)
//        .map(_.toList)
//        .fold(List.empty[Any])(identity)
//        .map(_.cast[Option[_]])
//        .filter(_.nonEmpty)
//        .flatten
//      js.head :: monthsQuestion :: js.tail
//    }
//
//    val formHandlers: List[String] = List(
//      routes.DeferredVatBillController.get().url,
//      routes.MonthsController.get().url,
//      routes.MonthsController.getInstallmentBreakdown().url,
//      routes.WhenToPayController.get().url
//    )
//
//    val firstUnfilledForm =
//      formPages
//        .zip(formHandlers).zipWithIndex
//        .find({
//          case (formPage, index) =>
//            (formPage._1.isEmpty || formPage._1.contains(-1)) && index < formHandlers.indexOf(request.uri)
//        })
//        .fold(request.uri)({case (a,_) => a._2})
//    if (firstUnfilledForm != request.uri) {
//      Some(Future.successful(Redirect(firstUnfilledForm)))
//    } else None
//  }
//
//}
//
//object JourneySession {
//  implicit val formats = Json.format[JourneySession]
//}