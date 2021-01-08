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

case class KnownFacts (key: String, value: String)

object KnownFacts {
  implicit val formats = Json.format[KnownFacts]
}

case class RootInterface (service: String, knownFacts: Seq[KnownFacts])

object RootInterface {
  implicit val formats = Json.format[RootInterface]
}