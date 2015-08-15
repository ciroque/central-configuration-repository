package org.ciroque.ccr

import akka.actor.ActorRefFactory
import org.ciroque.ccr.core.DataStoreResults.DataStoreResult
import org.ciroque.ccr.core.{DataStoreResults, Commons, SettingsDataStore}
import org.easymock.EasyMock._
import org.scalatest.mock._
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import spray.http.StatusCodes
import spray.testkit.ScalatestRouteTest

import scala.concurrent.Future

class ConfigurationProviderServiceTests
  extends FunSpec
  with ConfigurationProviderService
  with ScalatestRouteTest
  with Matchers
  with BeforeAndAfterEach
  with EasyMockSugar {

  override implicit val dataStore: SettingsDataStore = mock[SettingsDataStore]

  override def actorRefFactory: ActorRefFactory = system

  val settingsPath = s"/${Commons.rootPath}/${Commons.settingsSegment}"

  override def beforeEach() = {
    reset(dataStore)
  }

  describe("ConfigurationProviderService") {
    val environment = "global"
    val application = "application"
    val scope = "logging"

    val environments = List("global", "dev", "qa", "prod")
    val applications = List("web app", "service app", "mobile app")
    val scopes = List("logging", "settings", "user-settings")
    val settings = List("log-level", "logfilename", "roll-rate")

    describe("Non application routes") {
      it("should return a 418 I'm a Tea Pot on the root route") {
        Get("/") ~> routes ~> check {
          status should equal(Commons.teaPotStatusCode)
          responseAs[String] should include("/documentation")
          //          responseAs[RootResponse].message should include("please")
        }
      }

      it("should return a 418 I'm a Tea Pot on the root app route") {
        Get(s"/${Commons.rootPath}") ~> routes ~> check {
          status should equal(Commons.teaPotStatusCode)
        }
      }
    }

    describe("environment routes") {
      it("should return a list of environments") {
        verifyGetForPath(settingsPath, dataStore.retrieveEnvironments, environments)
      }

      it("should return a list of environments when given an ending slash") {
        verifyGetForPath(s"$settingsPath/", dataStore.retrieveEnvironments, environments)
      }
    }

    describe("application routes") {

      it("should return a list of applications given an environment") {
        verifyGetForPath(s"$settingsPath/$environment", dataStore.retrieveApplications(environment), applications)
      }

      it("should return a list of applications given an environment when given an ending slash") {
        verifyGetForPath(s"$settingsPath/$environment/", dataStore.retrieveApplications(environment), applications)
      }

      it("should return a 404 when the environment is not found") {
        expecting {
          dataStore.retrieveApplications(environment).andReturn(Future.successful(DataStoreResults.NotFound("environment", environment)))
        }
        whenExecuting(dataStore) {
          Get(s"$settingsPath/$environment") ~> routes ~> check {
            status should equal(StatusCodes.NotFound)
            responseAs[String] should include("environment")
            responseAs[String] should include(environment)
          }
        }
      }

      it("should return an empty array when there are no applications in the given environment") {
        expecting {
          dataStore.retrieveApplications(environment).andReturn(Future.successful(DataStoreResults.NoChildrenFound("environment", environment)))
        }
        whenExecuting(dataStore) {
          Get(s"$settingsPath/$environment") ~> routes ~> check {
            status should equal(StatusCodes.OK)
            responseAs[String] should include("[]")
            responseAs[String] should include("applications")
          }
        }
      }
    }

    describe("scope routes") {

      it("should return a list of scopes given an environment and an application") {
        verifyGetForPath(s"$settingsPath/$environment/$application", dataStore.retrieveScopes(environment, application), scopes)
      }

      it("should return a list of scopes given an environment and an application when given an ending slash") {
        verifyGetForPath(s"$settingsPath/$environment/$application", dataStore.retrieveScopes(environment, application), scopes)
      }

      it("should return a 404 when the application is not found") {
        expecting {
          dataStore.retrieveScopes(environment, application).andReturn(Future.successful(DataStoreResults.NotFound("application", application)))
        }
        whenExecuting(dataStore) {
          Get(s"$settingsPath/$environment/$application") ~> routes ~> check {
            status should equal(StatusCodes.NotFound)
            responseAs[String] should include("application")
            responseAs[String] should include(application)
          }
        }
      }

      it("should return an empty array when there are no scope in the given application") {
        expecting {
          dataStore.retrieveScopes(environment, application).andReturn(Future.successful(DataStoreResults.NoChildrenFound("application", application)))
        }
        whenExecuting(dataStore) {
          Get(s"$settingsPath/$environment/$application") ~> routes ~> check {
            status should equal(StatusCodes.OK)
            responseAs[String] should include("[]")
            responseAs[String] should include("scopes")
          }
        }
      }
    }

    describe("settings routes") {

      it("should return a list of settings given an environment, an application, and a scope") {
        verifyGetForPath(s"$settingsPath/$environment/$application/$scope", dataStore.retrieveSettings(environment, application, scope), settings)
      }

      it("should return a list of scopes given an environment, an application, and a scope when given an ending slash") {
        verifyGetForPath(s"$settingsPath/$environment/$application/$scope", dataStore.retrieveSettings(environment, application, scope), settings)
      }

      it("should return a 404 when the scope is not found") {
        expecting {
          dataStore.retrieveSettings(environment, application, scope).andReturn(Future.successful(DataStoreResults.NotFound("scope", scope)))
        }
        whenExecuting(dataStore) {
          Get(s"$settingsPath/$environment/$application/$scope") ~> routes ~> check {
            status should equal(StatusCodes.NotFound)
            responseAs[String] should include("scope")
            responseAs[String] should include(scope)
          }
        }
      }

      it("should return an empty array when there are no settings in the given scope") {
        expecting {
          dataStore.retrieveSettings(environment, application, scope).andReturn(Future.successful(DataStoreResults.NoChildrenFound("scope", scope)))
        }
        whenExecuting(dataStore) {
          Get(s"$settingsPath/$environment/$application/$scope") ~> routes ~> check {
            status should equal(StatusCodes.OK)
            responseAs[String] should include("[]")
            responseAs[String] should include("settings")
          }
        }
      }
    }
  }

  private def verifyGetForPath(path: String = settingsPath, retriever: => Future[DataStoreResult], listToReturn: List[String]): Unit = {
    expecting {
      retriever.andReturn(Future.successful(DataStoreResults.Found(listToReturn)))
    }
    whenExecuting(dataStore) {
      Get(path) ~> routes ~> check {
        status should equal(StatusCodes.OK)
        listToReturn.foreach { environment =>
          responseAs[String] should include(environment)
        }
      }
    }
  }
}
