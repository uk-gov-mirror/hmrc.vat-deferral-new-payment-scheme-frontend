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

import play.api.Logger
import play.api.libs.json._
import play.api.mvc.Results._
import play.api.mvc._
import shapeless.syntax.std.tuple._
import shapeless.syntax.typeable._
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.controllers.DateFormValues

import scala.concurrent.Future

object journey {
  type Form[+T] = Option[T]
  type Uri = String
}

case class FormPage[+T](
  uri: journey.Uri,
  value: journey.Form[T] = None
)

trait Journey {

  private val logger = Logger(getClass)

  def steps: List[Option[FormPage[_]]]

  val formPages: List[FormPage[_]] =
    steps
      .filter(_.nonEmpty)
      .flatten

  // TODO strip/add the server path prefix instead of endsWith
  def currentIndex(implicit request: Request[_]): Int =
    formPages.indexWhere(x => request.uri.endsWith(x.uri))

  def previous(implicit request: Request[_]): journey.Uri =
    formPages
      .zipWithIndex
      .find(_._2 == currentIndex -1)
      .fold(throw new IllegalArgumentException()){
        _._1.uri
      }

  def next(implicit request: Request[_]): Future[Result] =
    formPages
      .find(_.value.isEmpty)
      .map { fp =>
        Future.successful(
          Redirect(fp.uri)
            .withSession(request.session))
      }.getOrElse(throw new IllegalStateException(""))

  def isEmptyFormPage(
    indexedFormPage: (FormPage[_],Int)
  )(
    implicit request: Request[_]
  ): Boolean = indexedFormPage match {
    case (formPage, index) =>
      formPage.value.isEmpty && index < currentIndex
  }

  def redirect(implicit request: Request[AnyContent]): Option[Future[Result]] = {
    val firstUnfilledFormUri: journey.Uri =
      formPages
        .zipWithIndex
        .find(isEmptyFormPage)
        .fold(request.uri)({case (a,_) => a.uri})
    if (firstUnfilledFormUri != request.uri) {
      logger.info(s"redirecting ${request.uri} to $firstUnfilledFormUri")
      Some(Future.successful(Redirect(firstUnfilledFormUri).withSession(request.session)))
    }
    else None
  }

}

case class MatchingJourneySession (
  id: String,
  vrn: FormPage[String] = FormPage("enter-vrn"),
  postCode: FormPage[String] = FormPage("enter-post-code"),
  latestVatAmount: FormPage[String] = FormPage("enter-latest-vat-return-total"),
  latestAccountPeriodMonth: FormPage[String] = FormPage("enter-latest-vat-accounting-period"),
  date: FormPage[DateFormValues] = FormPage("enter-vat-registration-date"),
  isUserEnrolled: Boolean = false,
  failedMatchingAttempts: Int = 0
) extends Journey {

  // n.b. the session collection has a ttl of 900 seconds so no need to reset or compare any times
  def locked: Boolean = {
    failedMatchingAttempts == 3
  }

  // TODO - how to lift this to the trait
  def steps: List[Option[FormPage[_]]] =
    MatchingJourneySession
      .unapply(this)
      .map(_.toList)
      .fold(List.empty[Any])(identity)
      .map(_.cast[FormPage[_]])

}

object MatchingJourneySession {
  implicit val formPageStringFormat: OFormat[FormPage[String]] =
    Json.format[FormPage[String]]
  implicit val formPageDateFormValueFormat: OFormat[FormPage[DateFormValues]] =
    Json.format[FormPage[DateFormValues]]
  implicit val formats: OFormat[MatchingJourneySession] =
    Json.format[MatchingJourneySession]
}

//case class MatchingJourneySessionBar (
//  id: String,
//  vrn: journey.Form[String] = None,
//  postCode: journey.Form[String] = None,
//  latestVatAmount: journey.Form[String] = None,
//  latestAccountPeriodMonth: journey.Form[String] = None,
//  date: journey.Form[DateFormValues] = None,
//  isUserEnrolled: Boolean = false,
//  failedMatchingAttempts: Int = 0
//) {
//  // n.b. the session collection has a ttl of 900 seconds so no need to reset or compare any times
//  def locked: Boolean = {
//    failedMatchingAttempts == 3
//  }
//
//  def redirect(request: Request[AnyContent]): Option[Future[Result]] = {
//    val formPages: List[journey.Form[_]] =
//      MatchingJourneySessionBar
//        .unapply(this)
//        .map(_.toList)
//        .fold(List.empty[Any])(identity)
//        .map(_.cast[journey.Form[_]])
//        .filter(_.nonEmpty)
//        .flatten
//    val formHandlers: List[journey.Uri] = List(
//      routes.VrnController.get().url,
//      routes.PostCodeController.get().url,
//      routes.VatReturnController.get().url,
//      routes.VatPeriodController.get().url,
//      routes.VatRegistrationDateController.get().url
//    )
//    val firstUnfilledFormUri: journey.Uri =
//      formPages
//      .zip(formHandlers).zipWithIndex
//      .find({
//        case (formPage, index) =>
//          formPage._1.isEmpty &&
//            index < formHandlers.indexOf(request.uri)
//      })
//      .fold(request.uri)({case (a,_) => a._2})
//
//    if (firstUnfilledFormUri != request.uri)
//      Some(Future.successful(Redirect(firstUnfilledFormUri).withSession(request.session)))
//    else None
//  }
//}
//
//object MatchingJourneySessionBar {
//
//  implicit val formats: OFormat[MatchingJourneySessionBar] = Json.format[MatchingJourneySessionBar]
//}

