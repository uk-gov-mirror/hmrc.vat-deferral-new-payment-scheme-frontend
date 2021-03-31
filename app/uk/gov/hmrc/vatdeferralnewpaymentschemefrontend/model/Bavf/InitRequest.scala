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

import play.api.libs.json._

case class InitRequest(
  serviceIdentifier: String,
  continueUrl: String,
  prepopulatedData: Option[InitRequestPrepopulatedData],
  messages: Option[InitRequestMessages] = None,
  customisationsUrl: Option[String] = None,
  address: Option[InitRequestAddress] = None,
  bacsRequirements:InitBACSRequirements = InitBACSRequirements(true, false)
)

case class InitBACSRequirements(directDebitRequired: Boolean, directCreditRequired: Boolean)

object InitRequest {
  implicit val initBacsWrites: OWrites[InitBACSRequirements] = Json.writes[InitBACSRequirements]
  implicit val messagesWrites: OWrites[InitRequestMessages] = Json.writes[InitRequestMessages]
  implicit val addressWrites: OWrites[InitRequestAddress] = Json.writes[InitRequestAddress]
  implicit val prePopulatedDataWrites: OWrites[InitRequestPrepopulatedData] = Json.writes[InitRequestPrepopulatedData]
  implicit val writes: Writes[InitRequest] = Json.writes[InitRequest]
}