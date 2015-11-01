package org.ciroque.ccr

import akka.actor.ActorRefFactory
import org.ciroque.ccr.core.Commons
import org.ciroque.ccr.datastores.{DataStoreResults, SettingsDataStore}
import org.ciroque.ccr.logging.CachingLogger
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.models.ConfigurationFactory._
import org.ciroque.ccr.responses.{HyperMediaMessageResponse, ConfigurationUpdateResponse, ConfigurationResponse}
import org.easymock.EasyMock._
import org.joda.time.DateTime
import org.scalatest.mock.EasyMockSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.testkit.ScalatestRouteTest

import scala.concurrent.Future

class ConfigurationSchedulingServiceTests
  extends FunSpec
  with ConfigurationSchedulingService
  with ScalatestRouteTest
  with Matchers
  with BeforeAndAfterEach
  with EasyMockSugar {
  describe("ConfigurationSchedulingService") {
    import spray.json.JsString

    val schedulingPath = s"/${Commons.rootPath}/${Commons.schedulingSegment}"

    val testEnvironment = "test-environment"
    val testApplication = "test-application"
    val testScope = "test-scope"
    val testSetting = "test-setting"
    val testValue = JsString("test-value")
    val testEffectiveAt = DateTime.now().minusDays(30)
    val testExpiresAt = DateTime.now().plusDays(30)
    val testTimeToLive = 360000L

    val testConfiguration = ConfigurationFactory(
      testEnvironment,
      testApplication,
      testScope,
      testSetting,
      testValue,
      testEffectiveAt,
      testExpiresAt,
      testTimeToLive
    )

    describe("configuration creation") {
      it("should return a 201 Created when creation is successful") {
        expecting {
          dataStore
            .insertConfiguration(isA(classOf[Configuration]))
            .andReturn(Future.successful(DataStoreResults.Added(testConfiguration)))
        }
        whenExecuting(dataStore) {
          Post(s"$schedulingPath", testConfiguration) ~> routes ~> check {
            status should equal(StatusCodes.OK)
            import org.ciroque.ccr.responses.ConfigurationResponseProtocol._
            val returnedConfiguration = responseAs[ConfigurationResponse]
            returnedConfiguration.configuration.head.toJson should equal(testConfiguration.toJson)
          }
        }
      }

      it("should return a 500 Internal Server Error when there is a DataStore failure") {
        import org.ciroque.ccr.models.ConfigurationFactory._
        val expectedErrorMessage = "Bad Mojo"
        val expectedThrowable = new Exception("The underlying data store experienced an error")
        expecting {
          dataStore
            .insertConfiguration(isA(classOf[Configuration]))
            .andReturn(Future.successful(DataStoreResults.Failure(expectedErrorMessage, expectedThrowable)))
        }
        whenExecuting(dataStore) {
          Post(s"$schedulingPath/", testConfiguration) ~> routes ~> check {
            status should equal(StatusCodes.InternalServerError)
          }
        }
      }
    }

    describe("configuration update") {
      it("should return a 200 OK when the configuration is updated successfully") {
        val updatedConfiguration = testConfiguration.copy(value = JsString("UPDATED"))
        expecting {
          dataStore
            .updateConfiguration(isA(classOf[Configuration]))
            .andReturn(Future.successful(DataStoreResults.Updated(testConfiguration, updatedConfiguration)))
        }
        whenExecuting(dataStore) {
          Put(s"$schedulingPath/", updatedConfiguration) ~> routes ~> check {
            status should equal(StatusCodes.OK)
            import org.ciroque.ccr.responses.ConfigurationUpdateResponseProtocol._
            val configurationUpdateResponse = responseAs[ConfigurationUpdateResponse]
            configurationUpdateResponse.previous.toJson should equal(testConfiguration.toJson)
            configurationUpdateResponse.updated.toJson should equal(updatedConfiguration.toJson)
          }
        }
      }

      it("should return a 404 Not Found when the configuration to be updated is not found") {
        val updatedConfiguration = testConfiguration.copy(value = JsString("UPDATED"))
        expecting {
          dataStore
            .updateConfiguration(isA(classOf[Configuration]))
            .andReturn(Future.successful(DataStoreResults.NotFound(None, Commons.DatastoreErrorMessages.NotFoundError)))
        }
        whenExecuting(dataStore) {
          Put(s"$schedulingPath/", updatedConfiguration) ~> routes ~> check {
            status should equal(StatusCodes.NotFound)
            import org.ciroque.ccr.responses.HyperMediaResponseProtocol._
            val hypermediaMessageResponse = responseAs[HyperMediaMessageResponse]
            hypermediaMessageResponse.message should be(Commons.DatastoreErrorMessages.NotFoundError)
          }
        }
      }
    }
  }

  override implicit val dataStore: SettingsDataStore = mock[SettingsDataStore]
  override implicit val logger = new CachingLogger()

  override def beforeEach() =
    reset(dataStore)

  override def actorRefFactory: ActorRefFactory = system
}
