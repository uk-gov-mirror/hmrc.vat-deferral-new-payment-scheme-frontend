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

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.helpers

import play.api.i18n.Messages
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig

object PageTitle {
  def apply(pageHeadingKey: String, hasErrors: Boolean = false)(implicit messages: Messages, appConfig: AppConfig): Option[String] = {
    Some(s"${if(hasErrors){messages("common.error.title")}else{""}} ${messages(pageHeadingKey)} - ${messages("common.page.title")}")
  }
}
