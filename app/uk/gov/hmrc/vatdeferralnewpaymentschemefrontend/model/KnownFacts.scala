/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model
import play.api.libs.json.Json

case class KnownFacts (key: String, value: String)

object KnownFacts {
  implicit val formats = Json.format[KnownFacts]
}

case class RootInterface (service: String, knownFacts: Seq[KnownFacts])

object RootInterface {
  implicit val formats = Json.format[RootInterface]
}