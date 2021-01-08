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

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.model.Bavf

import play.api.libs.functional.syntax._
import play.api.libs.json._

sealed trait Account

case class BusinessCompleteResponse(companyName: String, sortCode: String, accountNumber: String) extends Account

case class PersonalCompleteResponse(accountName: String, sortCode: String, accountNumber: String) extends Account

object Account {

  implicit val businessReads: Reads[BusinessCompleteResponse] = (
      (__ \ "business" \ "companyName").read[String] and
      (__ \ "business" \ "sortCode").read[String] and
      (__ \ "business" \ "accountNumber").read[String]
    )(BusinessCompleteResponse.apply _)

  implicit val personalReads: Reads[PersonalCompleteResponse] = (
    (__ \ "personal" \ "accountName").read[String] and
    (__ \ "personal" \ "sortCode").read[String] and
    (__ \ "personal" \ "accountNumber").read[String]
    )(PersonalCompleteResponse.apply _)

  implicit val accountReads: Reads[Account] = Reads[Account](jsValue => {
    (jsValue \ "accountType").toEither
      .map(
        v =>
          v.as[String] match {
            case "business" => businessReads.map(_.asInstanceOf[Account]).reads(jsValue)
            case "personal" => personalReads.map(_.asInstanceOf[Account]).reads(jsValue)
            case t =>
              JsError(__ \ "accountType", s"unknown account type $t")
                .asInstanceOf[JsResult[Account]]
          }
      )
      .fold(v => JsError(JsPath, v).asInstanceOf[JsResult[Account]], v => v)
  })
}