/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.controllers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.config.AppConfig
import uk.gov.hmrc.vatdeferralnewpaymentschemefrontend.views.html.HelloWorldPage

class HelloWorldControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {
  private val fakeRequest = FakeRequest("GET", "/")

  private val env           = Environment.simple()
  private val configuration = Configuration.load(env)

  private val serviceConfig = new ServicesConfig(configuration)
  private val appConfig     = new AppConfig(configuration, serviceConfig)

  val helloWorldPage: HelloWorldPage = app.injector.instanceOf[HelloWorldPage]

  "GET /" should {
    "return 200" in {

    }
  }
}
