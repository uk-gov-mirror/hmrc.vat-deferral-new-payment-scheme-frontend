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

import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request, Result}

import scala.concurrent.Future

case class JourneySession (
  id: String,
  eligible: Boolean = false,
  outStandingAmount: Option[BigDecimal] = None,
  numberOfPaymentMonths: Option[Int] = None,
  dayOfPayment: Option[Int] = None
) {
  def monthsQuestion: Option[Boolean] = numberOfPaymentMonths match {
    case Some(11) => Some(true)
    case Some(_) => Some(false)
    case _ => None
  }

  def redirect(request: Request[AnyContent]): Option[Future[Result]] = {
    import play.api.mvc.Results.Redirect
    import shapeless.syntax.std.tuple._
    import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.controllers.routes

    import scala.concurrent.Future
    val argList: List[Option[_]] =
      monthsQuestion ::
      JourneySession
        .unapply(this)
        .map(_.toList)
        .fold(List.empty[Any])(identity)
        .slice(3,5)
        .map(_.asInstanceOf[Option[_]])
    val routeList: List[String] = List(
      routes.MonthsController.get().url,
      routes.MonthsController.getInstallmentBreakdown().url,
      routes.WhenToPayController.get().url
    )
    val route =
      argList
        .zip(routeList).zipWithIndex
        .find({case (x,i) => (x._1.isEmpty || x._1.contains(-1)) && i < routeList.indexOf(request.uri)})
        .fold(request.uri)({case (a,_) => a._2})
    if (route != request.uri) {
      Some(Future.successful(Redirect(route)))
    } else None
  }

}

object JourneySession {
  implicit val formats = Json.format[JourneySession]
}