package org.ciroque.ccr

import akka.actor.ActorRefFactory
import org.ciroque.ccr.core.{Commons, SettingsDataStore}
import org.ciroque.ccr.responses.SettingResponse
import org.specs2.mutable.Specification
import spray.http.HttpHeaders.RawHeader
import spray.http._
import spray.testkit.Specs2RouteTest

class ConfigurationProviderServiceSpec
  extends Specification
  with Specs2RouteTest
  with ConfigurationProviderService {

  val HTTP_SUCCESS_STATUS = 200
  val HTTP_NOT_FOUND_STATUS = 404

  val settingsPath = s"${Commons.rootPath}/${Commons.settingsSegment}"

  override implicit val dataStore: SettingsDataStore = new MockSettingsDataStore()

  def assertCorsHeaders(headers: List[HttpHeader]) = {
    headers.contains(RawHeader("Access-Control-Allow-Headers", "Content-Type"))
    headers.contains(RawHeader("Access-Control-Allow-Methods", "GET"))
    headers.contains(RawHeader("Access-Control-Allow-Origin", "*"))
  }

  def actorRefFactory: ActorRefFactory = system

  "ConfigurationProviderService" should {
    "return a silly message on the default route" in {
      Get("/") ~> routes ~> check {
        status.intValue must_== 200
        assertCorsHeaders(headers)
        responseAs[String] must contain("Please review the documentation to learn how to use this service.")
        responseAs[String] must contain("/documentation")
      }
    }

    "handle root endpoint requests" in {
      def runAssertsAgainst(uri: String) = {
        Get(uri) ~> routes ~> check {
          status.intValue must_== HTTP_SUCCESS_STATUS
          assertCorsHeaders(headers)
          val responseString = responseAs[String]
          responseString must contain("dev")
          responseString must contain("qa")
          responseString must contain("prod")
          responseString must contain("global")
        }
      }

      "return a list of environments" in {
        runAssertsAgainst(s"/$settingsPath")
      }

      "return a list of environments with an ending slash" in {
        runAssertsAgainst(s"/$settingsPath/")
      }
    }

    "handle environment endpoint requests" in {
      "return a list of applications" in {
        Get(s"/$settingsPath/dev") ~> routes ~> check {
          status.intValue must_== HTTP_SUCCESS_STATUS
          assertCorsHeaders(headers)
          val responseString = responseAs[String]
          responseString must contain("ui")
          responseString must contain("svc")
        }
      }

      "return a list of applications given an environment with a trailing slash" in {
        Get(s"/$settingsPath/dev/") ~> routes ~> check {
          status.intValue must_== HTTP_SUCCESS_STATUS
          assertCorsHeaders(headers)
          val responseString = responseAs[String]
          responseString must contain("ui")
          responseString must contain("svc")
        }
      }
    }

    "handle application endpoint requests" in {
      "return a list of scopes" in {
        Get(s"/$settingsPath/prod/svc") ~> routes ~> check {
          status.intValue must_== HTTP_SUCCESS_STATUS
          assertCorsHeaders(headers)
          val responseString = responseAs[String]
          responseString must contain("logging")
          responseString must contain("global")
        }
      }

      "return a list of scopes given an environment and application with a trailing slash" in {
        Get(s"/$settingsPath/prod/svc/") ~> routes ~> check {
          status.intValue must_== HTTP_SUCCESS_STATUS
          assertCorsHeaders(headers)
          val responseString = responseAs[String]
          responseString must contain("logging")
          responseString must contain("global")
        }
      }
    }

    "handle scope endpoint requests" in {
      "return a list of settings" in {
        Get(s"/$settingsPath/dev/ui/logging") ~> routes ~> check {
          status.intValue must_== HTTP_SUCCESS_STATUS
          assertCorsHeaders(headers)
          val responseString = responseAs[String]
          responseString must contain("log-level")
          responseString must contain("logfile")
        }
      }

      "return a list of settings given an environment and application with a trailing slash" in {
        Get(s"/$settingsPath/prod/svc/global/") ~> routes ~> check {
          status.intValue must_== HTTP_SUCCESS_STATUS
          assertCorsHeaders(headers)
          val responseString = responseAs[String]
          responseString must contain("timeout")
          responseString must contain("app-skin")
        }
      }
    }

    "handle setting endpoint requests" in {
      "return the setting" in {
        import spray.json._
        Get(s"/$settingsPath/dev/ui/logging/log-level") ~> routes ~> check {
          status.intValue must_== HTTP_SUCCESS_STATUS
          assertCorsHeaders(headers)
          val mockDatastore = dataStore.asInstanceOf[MockSettingsDataStore]
          responseAs[String] must_== SettingResponse(Some(mockDatastore.primarySetting)).toJson.prettyPrint
        }
      }

      "return the setting with a trailing slash" in {
        import spray.json._
        Get(s"/$settingsPath/dev/ui/logging/log-level/") ~> routes ~> check {
          status.intValue must_== HTTP_SUCCESS_STATUS
          assertCorsHeaders(headers)
          val mockDatastore = dataStore.asInstanceOf[MockSettingsDataStore]
          responseAs[String] must_== SettingResponse(Some(mockDatastore.primarySetting)).toJson.prettyPrint
        }
      }
    }

    "404's" in {
      "environment not found" in {
        Get(s"/$settingsPath/404") ~> routes ~> check {
          status.intValue must_== HTTP_NOT_FOUND_STATUS
          assertCorsHeaders(headers)
        }
      }
      "application not found" in {
        Get(s"/$settingsPath/404/404") ~> routes ~> check {
          status.intValue must_== HTTP_NOT_FOUND_STATUS
          assertCorsHeaders(headers)
        }
      }
      "scope not found" in {
        Get(s"/$settingsPath/404/404/404") ~> routes ~> check {
          status.intValue must_== HTTP_NOT_FOUND_STATUS
          assertCorsHeaders(headers)
        }
      }
      "setting not found" in {
        Get(s"/$settingsPath/404/404/404/404") ~> routes ~> check {
          status.intValue must_== HTTP_NOT_FOUND_STATUS
          assertCorsHeaders(headers)
        }
      }
    }
  }
}
