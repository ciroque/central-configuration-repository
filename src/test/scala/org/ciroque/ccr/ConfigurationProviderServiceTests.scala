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

    val environments = List("global", "dev", "qa", "prod")

    describe("environment routes") {
      it("should return a list of environments") {
        verifyGetForPath(settingsPath, dataStore.retrieveEnvironments, environments)
      }

      it("should return a list of environments when given an ending slash") {
        verifyGetForPath(s"$settingsPath/", dataStore.retrieveEnvironments, environments)
      }
    }

    describe("application routes") {
      val environment = "global"
      val applications = List("web app", "service app", "mobile app")

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
