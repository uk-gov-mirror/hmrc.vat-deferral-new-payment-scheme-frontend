/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.data.Forms.text
import play.api.data.Mapping
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

@Singleton
class BaseController @Inject()(mcc: MessagesControllerComponents) extends FrontendController(mcc) with I18nSupport {

  protected def mandatory(key: String): Mapping[String] = {
    text.transform[String](_.trim, s => s).verifying(required(key))
  }

  protected def mandatoryAndValid(key: String, regex: String): Mapping[String] = {
    text.transform[String](_.trim, s => s).verifying(combine(required(key), constraint(key, regex)))
  }

  private def combine[T](c1: Constraint[T], c2: Constraint[T]): Constraint[T] = Constraint { v =>
    c1.apply(v) match {
      case Valid => c2.apply(v)
      case i: Invalid => i
    }
  }

  private def required(key: String): Constraint[String] = Constraint {
    case "" => Invalid(s"error.$key.required")
    case _ => Valid
  }

  private def constraint(key: String, regex: String): Constraint[String] = Constraint {
    case a if !a.matches(regex) => Invalid(s"error.$key.invalid")
    case _ => Valid
  }
}