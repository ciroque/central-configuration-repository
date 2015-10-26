package org.ciroque.ccr

import akka.actor.ActorRefFactory
import org.ciroque.ccr.core.Commons
import org.ciroque.ccr.datastores.{DataStoreResults, SettingsDataStore}
import org.ciroque.ccr.logging.CachingLogger
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.responses.ConfigurationResponse
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

    val settingsPath = s"/${Commons.rootPath}/${Commons.schedulingSegment}"

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
        import org.ciroque.ccr.models.ConfigurationFactory._
        expecting {
          dataStore
            .upsertConfiguration(isA(classOf[Configuration]))
            .andReturn(Future.successful(DataStoreResults.Added(testConfiguration)))
        }
        whenExecuting(dataStore) {
          Post(s"$settingsPath", testConfiguration) ~> routes ~> check {
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
            .upsertConfiguration(isA(classOf[Configuration]))
            .andReturn(Future.successful(DataStoreResults.Failure(expectedErrorMessage, expectedThrowable)))
        }
        whenExecuting(dataStore) {
          Post(s"$settingsPath/", testConfiguration) ~> routes ~> check {
            status should equal(StatusCodes.InternalServerError)
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
