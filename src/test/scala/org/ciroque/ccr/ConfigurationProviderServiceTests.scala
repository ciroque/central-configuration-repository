package org.ciroque.ccr

import akka.actor.ActorRefFactory
import org.ciroque.ccr.core.Commons
import org.ciroque.ccr.datastores.DataStoreResults.DataStoreResult
import org.ciroque.ccr.datastores.{DataStoreResults, SettingsDataStore}
import org.ciroque.ccr.logging.CachingLogger
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.responses.{ConfigurationResponse, HyperMediaMessageResponse, InternalServerErrorResponse}
import org.ciroque.ccr.stats.AccessStatsClient
import org.easymock.EasyMock._
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.mock._
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import spray.http.HttpHeaders.RawHeader
import spray.http.{HttpHeader, StatusCodes}
import spray.httpx.SprayJsonSupport._
import spray.json._
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
  override implicit val accessStatsClient: AccessStatsClient = mock[AccessStatsClient]
  override implicit val logger = new CachingLogger()
  val settingsPath = s"/${Commons.rootPath}/${Commons.settingsSegment}"

  override def actorRefFactory: ActorRefFactory = system

  override def beforeEach() = {
    reset(dataStore)
    reset(accessStatsClient)
    logger.reset()
  }

  describe("ConfigurationProviderService") {
    val environment = "global"
    val application = Commons.KeyStrings.ApplicationKey
    val scope = "logging"
    val setting = "log-level"
    val sourceId = "SOME_SOURCEID"

    val environments = List("global", "dev", "qa", "prod")
    val applications = List("web app", "service app", "mobile app")
    val scopes = List("logging", "settings", "user-settings")
    val settings = List("log-level", "logfilename", "roll-rate")

    describe("Non application routes") {
      it("should return a 418 I'm a Tea Pot on the root route") {
        assertHyperMediaResponseOnRoots("/")
      }

      it("should return a 418 I'm a Tea Pot on the root app route") {
        assertHyperMediaResponseOnRoots(s"/${Commons.rootPath}")
      }

      def assertHyperMediaResponseOnRoots(path: String) = {
        Get(path) ~> routes ~> check {
          status should equal(Commons.teaPotStatusCode)
          assertCorsHeaders(headers)
          import org.ciroque.ccr.responses.HyperMediaResponseProtocol._
          val hyperMediaResponse = responseAs[HyperMediaMessageResponse]
          hyperMediaResponse.message should include("Please review the documentation")
          hyperMediaResponse._links.size should equal(1)
          hyperMediaResponse._links.head._1 should equal("documentation")
          hyperMediaResponse._links.head._2 should equal("/documentation")
        }
      }
    }

    describe("environment routes") {
      it("should return a list of environments") {
        verifyGetForPath("", dataStore.retrieveEnvironments(), environments)
        assertLogEvents("ConfigurationProviderService::environmentsRoute", 1)
      }

      it("should return a list of environments with a trailing slash") {
        verifyGetForPath("/", dataStore.retrieveEnvironments(), environments)
        assertLogEvents("ConfigurationProviderService::environmentsRoute", 1)
      }

      it("should respond with a 500 and general messaging when a DataStore failure occurs") {
        verifyGetWithDataStoreFailure("", dataStore.retrieveEnvironments())
        assertLogEvents("ConfigurationProviderService::environmentsRoute", 1)
      }

      it("should respond with a 500 and general messaging when a DataStore failure occurs with trailing slash") {
        verifyGetWithDataStoreFailure("/", dataStore.retrieveEnvironments())
        assertLogEvents("ConfigurationProviderService::environmentsRoute", 1)
      }
    }

    describe("application routes") {
      it("should return a list of applications given an environment") {
        verifyGetForPath(s"/$environment", dataStore.retrieveApplications(environment), applications)
        assertLogEvents("ConfigurationProviderService::applicationsRoute", 1, environment)
      }

      it("should return a list of applications given an environment with a trailing slash") {
        verifyGetForPath(s"/$environment/", dataStore.retrieveApplications(environment), applications)
        assertLogEvents("ConfigurationProviderService::applicationsRoute", 1, environment)
      }

      it("should return a 404 when the environment is not found") {
        expecting {
          dataStore.retrieveApplications(environment).andReturn(Future.successful(DataStoreResults.NotFound(s"environment '$environment' was not found")))
        }
        whenExecuting(dataStore) {
          Get(s"$settingsPath/$environment") ~> routes ~> check {
            status should equal(StatusCodes.NotFound)
            assertCorsHeaders(headers)
            responseAs[String] should include(Commons.KeyStrings.EnvironmentKey)
            responseAs[String] should include(environment)
          }
        }
        assertLogEvents("ConfigurationProviderService::applicationsRoute", 1, environment)
      }

      it("should respond with a 500 and general messaging when a DataStore failure occurs") {
        verifyGetWithDataStoreFailure(s"/$environment", dataStore.retrieveApplications(environment))
        assertLogEvents("ConfigurationProviderService::applicationsRoute", 1, environment)
      }

      it("should respond with a 500 and general messaging when a DataStore failure occurs with a trailing slash") {
        verifyGetWithDataStoreFailure(s"/$environment/", dataStore.retrieveApplications(environment))
        assertLogEvents("ConfigurationProviderService::applicationsRoute", 1, environment)
      }
    }

    describe("scope routes") {
      it("should return a list of scopes given an environment and an application") {
        verifyGetForPath(s"/$environment/$application", dataStore.retrieveScopes(environment, application), scopes)
        assertLogEvents("ConfigurationProviderService::scopesRoute", 1, environment, application)
      }

      it("should return a list of scopes given an environment and an application with a trailing slash") {
        verifyGetForPath(s"/$environment/$application", dataStore.retrieveScopes(environment, application), scopes)
        assertLogEvents("ConfigurationProviderService::scopesRoute", 1, environment, application)
      }

      it("should return a 404 when the application is not found") {
        val notFoundMessage: String = s"application '$application' was not found"
        expecting {
          dataStore.retrieveScopes(environment, application).andReturn(Future.successful(DataStoreResults.NotFound(notFoundMessage)))
        }
        whenExecuting(dataStore) {
          Get(s"$settingsPath/$environment/$application") ~> routes ~> check {
            status should equal(StatusCodes.NotFound)
            assertCorsHeaders(headers)
            responseAs[String] should include(notFoundMessage)
          }
        }
        assertLogEvents("ConfigurationProviderService::scopesRoute", 1, environment, application)
      }

      it("should respond with a 500 and general messaging when a DataStore failure occurs") {
        verifyGetWithDataStoreFailure(s"/$environment/$application", dataStore.retrieveScopes(environment, application))
        assertLogEvents("ConfigurationProviderService::scopesRoute", 1, environment, application)
      }

      it("should respond with a 500 and general messaging when a DataStore failure occurs with trailing slash") {
        verifyGetWithDataStoreFailure(s"/$environment/$application", dataStore.retrieveScopes(environment, application))
        assertLogEvents("ConfigurationProviderService::scopesRoute", 1, environment, application)
      }
    }

    describe("settings routes") {
      it("should return a list of settings given an environment, an application, and a scope") {
        verifyGetForPath(s"/$environment/$application/$scope", dataStore.retrieveSettings(environment, application, scope), settings)
        assertLogEvents("ConfigurationProviderService::settingsRoute", 1, environment, application, scope)
      }

      it("should return a list of settings given an environment, an application, and a scope with a trailing slash") {
        verifyGetForPath(s"/$environment/$application/$scope", dataStore.retrieveSettings(environment, application, scope), settings)
        assertLogEvents("ConfigurationProviderService::settingsRoute", 1, environment, application, scope)
      }

      it("should return a 404 when the scope is not found") {
        val notFoundMessage: String = "scope '$scope' was not found"
        expecting {
          dataStore.retrieveSettings(environment, application, scope).andReturn(Future.successful(DataStoreResults.NotFound(notFoundMessage)))
        }
        whenExecuting(dataStore) {
          Get(s"$settingsPath/$environment/$application/$scope") ~> routes ~> check {
            status should equal(StatusCodes.NotFound)
            assertCorsHeaders(headers)
            responseAs[String] should include(notFoundMessage)
          }
        }
        assertLogEvents("ConfigurationProviderService::settingsRoute", 1, environment, application, scope)
      }

      it("should respond with a 500 and general messaging when a DataStore failure occurs") {
        verifyGetWithDataStoreFailure(s"/$environment/$application/$scope", dataStore.retrieveSettings(environment, application, scope))
        assertLogEvents("ConfigurationProviderService::settingsRoute", 1, environment, application, scope)
      }

      it("should respond with a 500 and general messaging when a DataStore failure occurs with trailing slash") {
        verifyGetWithDataStoreFailure(s"/$environment/$application/$scope/", dataStore.retrieveSettings(environment, application, scope))
        assertLogEvents("ConfigurationProviderService::settingsRoute", 1, environment, application, scope)
      }
    }

    describe("configuration route") {
      import java.util.UUID
      val effectiveAt = DateTime.now().minusDays(30)
      val expiresAt = DateTime.now().plusDays(30)
      val ttl = 60 * 60 * 24 // 24 little hours
      val settingUri = s"$settingsPath/$environment/$application/$scope/$setting"
      val configuration = ConfigurationFactory(
        UUID.randomUUID,
        environment,
        application,
        scope,
        setting,
        Some(sourceId),
        "DEBUG",
        effectiveAt,
        expiresAt,
        ttl)

      it("returns the configuration") {
        assertConfigurationEndpoint(settingUri)
        assertLogEvents("ConfigurationProviderService::configurationRoute", 1, environment, application, scope, setting)
      }

      it("returns the configuration with ending slash") {
        assertConfigurationEndpoint(s"$settingUri/")
        assertLogEvents("ConfigurationProviderService::configurationRoute", 1, environment, application, scope, setting)
      }

      it("should return a 404 when the setting name is not found") {
        val notFoundMessage: String = s"environment '$environment' / application '$application' / scope '$scope' / setting '$setting' combination was not found"
        expecting {
          dataStore.retrieveConfiguration(environment, application, scope, setting, None).andReturn(Future.successful(DataStoreResults.NotFound(notFoundMessage)))
        }
        whenExecuting(dataStore) {
          Get(settingUri) ~> routes ~> check {
            status should equal(StatusCodes.NotFound)
            assertCorsHeaders(headers)
            import org.ciroque.ccr.responses.HyperMediaResponseProtocol._
            val responseBody = responseAs[HyperMediaMessageResponse]
            responseBody.message should include(notFoundMessage)
            responseBody.message should include(environment)
            responseBody.message should include(application)
            responseBody.message should include(scope)
            responseBody.message should include(setting)
          }
        }
        assertLogEvents("ConfigurationProviderService::configurationRoute", 1, environment, application, scope, setting)
      }

      it("should respond with a 500 and general messaging when a DataStore failure occurs") {
        verifyGetWithDataStoreFailure(s"/$environment/$application/$scope/$setting", dataStore.retrieveConfiguration(environment, application, scope, setting, None))
        assertLogEvents("ConfigurationProviderService::configurationRoute", 1, environment, application, scope, setting)
      }

      it("should respond with a 500 and general messaging when a DataStore failure occurs with trailing slash") {
        verifyGetWithDataStoreFailure(s"/$environment/$application/$scope/$setting/", dataStore.retrieveConfiguration(environment, application, scope, setting, None))
        assertLogEvents("ConfigurationProviderService::configurationRoute", 1, environment, application, scope, setting)
      }

      it("returns a single Configuration matching on the source id") {
        val uri = s"$settingUri?sourceId=$sourceId"

        expecting {
          dataStore.retrieveConfiguration(environment, application, scope, setting, Some(sourceId)).andReturn(futureSuccessfulDataStoreResult(List(configuration)))
        }
        whenExecuting(dataStore) {
          Get(uri) ~> routes ~> check {
            status should equal(StatusCodes.OK)
          }
        }
      }

      def assertConfigurationEndpoint(uri: String): Unit = {
        expecting {
          dataStore.retrieveConfiguration(environment, application, scope, setting, None).andReturn(futureSuccessfulDataStoreResult(List(configuration)))
        }
        whenExecuting(dataStore) {
          Get(uri) ~> routes ~> check {
            status should equal(StatusCodes.OK)
            assertCorsHeaders(headers)
            import org.ciroque.ccr.responses.ConfigurationResponseProtocol._
            val conf = responseAs[ConfigurationResponse]
            conf.configuration.size should equal(1)
            conf.configuration.head.toJson.toString should equal(configuration.toJson.toString())
            conf.configuration.head._id.toString should fullyMatch regex "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"

            val cacheUntil = DateTime.now(DateTimeZone.UTC).plusSeconds(conf.configuration.head.temporality.ttl.toInt)
            val expiresHeader = headers.filter(header => header.name == "Expires")
            expiresHeader.size shouldEqual 1

            val expiry = DateTime.parse(expiresHeader.head.value, Commons.DateTimeFormatter1123)
            cacheUntil.getMillis shouldBe (expiry.getMillis +- 1000)

            val maxAgeHeader = headers.filter(header => header.name.contains("Cache-Control"))
            maxAgeHeader.size shouldEqual 1
            maxAgeHeader.head.value should include(configuration.temporality.ttl.toString)
          }
        }
      }
    }
  }

  private def assertLogEvents(name: String, count: Int, shouldInclude: String*) = {
    logger.getEvents.size shouldBe count
    val logEvent = logger.getEvents.head
    (name +: shouldInclude).foreach(expected => logEvent should include(expected))
  }

  private def futureSuccessfulDataStoreResult[T](items: List[T]) = {
    Future.successful(DataStoreResults.Found(items))
  }

  private def verifyGetForPath(path: String, retriever: => Future[DataStoreResult], listToReturn: List[String]): Unit = {
    val pathAsList = splitPathToArray(path)
    expecting {
      retriever.andReturn(Future.successful(DataStoreResults.Found(listToReturn)))
      accessStatsClient.recordQuery(pathAsList.head, pathAsList(1), pathAsList(2), pathAsList(3)).andReturn(Future.successful(1L))
    }
    whenExecuting(dataStore, accessStatsClient) {
      Get(s"$settingsPath$path") ~> routes ~> check {
        status should equal(StatusCodes.OK)
        assertCorsHeaders(headers)
        listToReturn.foreach(segment => responseAs[String] should include(segment))
      }
    }
  }

  private def splitPathToArray(path: String): List[String] = {
    val segments = List("", "", "", "")

    val cleaned = path
      .substring(if (path.startsWith("/")) 1 else 0)
      .split('/')
      .toList

    cleaned.length match {
      case 4 => cleaned
      case 3 => cleaned ++ segments.drop(3)
      case 2 => cleaned ++ segments.drop(2)
      case 1 => if (cleaned.head == "") segments else cleaned ++ segments.drop(1)
      case 0 => segments
    }
  }

  private def verifyGetWithDataStoreFailure(path: String, retriever: => Future[DataStoreResult]) = {
    val errorMessage = "Mock Failure"
    val cause = new Exception("The underlying data store was not available.")
    expecting {
      retriever.andReturn(Future.successful(DataStoreResults.Failure(errorMessage, cause)))
    }
    whenExecuting(dataStore) {
      Get(s"$settingsPath$path") ~> routes ~> check {
        status should equal(StatusCodes.InternalServerError)
        assertCorsHeaders(headers)
        import org.ciroque.ccr.responses.InternalServerErrorResponseProtocol._
        val internalServerErrorResponse = responseAs[InternalServerErrorResponse]
        internalServerErrorResponse.message should equal(errorMessage)
        internalServerErrorResponse.cause should equal(cause.getMessage)
      }
    }
  }

  private def assertCorsHeaders(headers: List[HttpHeader]) = {
    headers should contain(RawHeader("Access-Control-Allow-Headers", "Content-Type"))
    headers should contain(RawHeader("Access-Control-Allow-Methods", "GET,PUT,POST"))
    headers should contain(RawHeader("Access-Control-Allow-Origin", "*"))
  }
}
