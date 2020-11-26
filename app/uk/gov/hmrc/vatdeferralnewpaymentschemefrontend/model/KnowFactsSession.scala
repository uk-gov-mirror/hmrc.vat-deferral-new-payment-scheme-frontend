/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model
import play.api.libs.json.Json
import play.api.mvc

case class KnownFactsSession(vrn: String, postCode: Option[String] = None, lastestVatAmount: Option[String] = None, latestAccountPeriodMonth: Option[String] = None, date: Option[String] = None, isUserEnrolled: Boolean = false)

object KnownFactsSession {
  implicit val formats = Json.format[KnownFactsSession]

  def convertFromJson(pd: String): Option[KnownFactsSession] = {
    val js = Json.parse(pd)
    Json.fromJson[KnownFactsSession](js).fold(
      _ => None,
      valid => {
        Some(valid)
      })
  }

  def convertToJson(knownFactsSession: KnownFactsSession) = {
    Json.toJson(knownFactsSession).toString()
  }
}

object RequestSession {
  def getObject(session: play.api.mvc.Session): Option[KnownFactsSession] = {
    val knownFactsSessionJson: Option[String] = {
      for {
        kf <- session.get("knownFactsSession")
      } yield {
        kf
      }
    }

    knownFactsSessionJson match {
      case Some(knownFactsSessionJson) => {
        val knownFactsSession = KnownFactsSession.convertFromJson(knownFactsSessionJson)
        knownFactsSession match {
          case Some(knownFactsSession) => Some(knownFactsSession)
          case None => None
        }
      }

      case None => None
    }
  }
}

case class KnownFacts (key: String, value: String)

object KnownFacts {
  implicit val formats = Json.format[KnownFacts]
}

case class RootInterface (service: String, knownFacts: Seq[KnownFacts])

object RootInterface {
  implicit val formats = Json.format[RootInterface]
}