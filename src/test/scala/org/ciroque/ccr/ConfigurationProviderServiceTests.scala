package org.ciroque.ccr

import akka.actor.ActorRefFactory
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
    println("dataStore has been reset")
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
    describe("environment routes") {
      it("should return a list of environments") {
        expecting {
          dataStore.retrieveEnvironments().andReturn(Future.successful(DataStoreResults.Found(List("global", "dev", "qa", "prod"))))
        }
        whenExecuting(dataStore) {
          Get(settingsPath) ~> routes ~> check {
            status should equal(StatusCodes.OK)
            responseAs[String] should include("global")
            responseAs[String] should include("dev")
            responseAs[String] should include("qa")
            responseAs[String] should include("prod")
          }
        }
      }

      it("should return a list of environments when given an ending slash") {
        expecting {
          dataStore.retrieveEnvironments().andReturn(Future.successful(DataStoreResults.Found(List("global", "dev", "qa", "prod"))))
        }
        whenExecuting(dataStore) {
          Get(s"$settingsPath/") ~> routes ~> check {
            status should equal(StatusCodes.OK)
            responseAs[String] should include("global")
            responseAs[String] should include("dev")
            responseAs[String] should include("qa")
            responseAs[String] should include("prod")
          }
        }
      }
    }

    describe("application routes") {

    }
  }
}
