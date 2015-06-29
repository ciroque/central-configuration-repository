package org.ciroque.ccr

import akka.actor.ActorRefFactory
import org.ciroque.ccr.core.{Commons, SettingsDataStore}
import org.specs2.mutable.Specification
import spray.http.HttpHeaders.RawHeader
import spray.http._
import spray.testkit.Specs2RouteTest

class ConfigurationProviderServiceSpec
  extends Specification
  with Specs2RouteTest
  with ConfigurationProviderService {

  val HTTP_SUCCESS_STATUS = 200

  val settingsPath = s"${Commons.rootPath}/${Commons.settingsSegment}"

  override implicit val dataStore: SettingsDataStore = new MockSettingsDataStore()

  def assertCorsHeaders(headers: List[HttpHeader]) = {
    headers.contains(RawHeader("Access-Control-Allow-Headers", "Content-Type"))
    headers.contains(RawHeader("Access-Control-Allow-Methods", "GET"))
    headers.contains(RawHeader("Access-Control-Allow-Origin", "*"))
  }

  def actorRefFactory: ActorRefFactory = system

  "ConfigurationProviderService" should {
    "return a silly message on the root route" in {
      Get("/") ~> routes ~> check {
        status.intValue must_== 200
        assertCorsHeaders(headers)
        responseAs[String] must contain("Please review the documentation to learn how to use this service.")
        responseAs[String] must contain("/documentation")
      }
    }

    "handle environment endpoint requests" in {
      def runAssertsAgainst(uri: String) = {
        Get(uri) ~> routes ~> check {
          status.intValue must_== HTTP_SUCCESS_STATUS
          assertCorsHeaders(headers)
          val responseString = responseAs[String]
          responseString must contain("dev")
          responseString must contain("qa")
          responseString must contain("beta")
          responseString must contain("staging")
          responseString must contain("prod")
        }
      }

      "return a list of environments" in {
        runAssertsAgainst(s"/$settingsPath")
      }

      "return a list of environments using an ending slash" in {
        runAssertsAgainst(s"/$settingsPath/")
      }
    }

    "handle application endpoint requests" in {
      "return a list of applications" in {
        Get(s"/$settingsPath/dev") ~> routes ~> check {
          status.intValue must_== HTTP_SUCCESS_STATUS
          assertCorsHeaders(headers)
          val responseString = responseAs[String]
          responseString must contain("dev-app-one")
          responseString must contain("dev-app-two")
        }
      }

      "return a list of applications using ending slash" in {
        Get(s"/$settingsPath/dev/") ~> routes ~> check {
          status.intValue must_== HTTP_SUCCESS_STATUS
          assertCorsHeaders(headers)
          val responseString = responseAs[String]
          responseString must contain("dev-app-one")
          responseString must contain("dev-app-two")
        }
      }
    }
  }
}
