package org.ciroque.ccr.datastores

import org.ciroque.ccr.datastores.DataStoreResults.{Found, NotFound}
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.models.ConfigurationFactory.Configuration
import org.joda.time.DateTime
import org.scalatest._
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.Span

abstract class SettingsDataStoreTests
  extends FunSpec
  with Matchers
  with ScalaFutures
  with BeforeAndAfterAll
  with BeforeAndAfterEach {

  import org.slf4j.Logger

  implicit val settingsDataStore: SettingsDataStore
  implicit val logger: Logger

  val testEnvironment = "test-environment"
  val testApplication = "test-application"
  val testScope = "test-scope"
  val testSetting = "test-settings"
  val testValue = "test-value"
  val testEffectiveAt = DateTime.now().minusMonths(1)
  val testExpiresAt = DateTime.now().plusMonths(1)
  val ttl = 360000

  val prodEnvironment: String = "prod"
  val qaEnvironment: String = "qa"
  val devEnvironment: String = "dev"
  val application: String = "app"
  val application2: String = "app2"
  val application3: String = "app3"
  val application4: String = "app4"
  val loggingScope: String = "logging"
  val logLevelSetting: String = "loglevel"
  val logRotationSetting: String = "logrotation"
  val logFilenameSetting: String = "logfilename"

  val nonExistentSegment: String = "non-existent"

  val testConfiguration = ConfigurationFactory(
    testEnvironment,
    testApplication,
    testScope,
    testSetting,
    testValue,
    testEffectiveAt,
    testExpiresAt,
    ttl
  )

  val activeLogLevelConfiguration: Configuration = ConfigurationFactory(prodEnvironment, application3, loggingScope, logLevelSetting, "ALL", DateTime.now().minusYears(1), DateTime.now().plusDays(7), 360000L)
  val defaultLogRotationConfig: Configuration = ConfigurationFactory("default", "app4", loggingScope, logRotationSetting, "12hours", DateTime.now().plusDays(7), DateTime.now().plusYears(1), 360000L)

  override def beforeEach(): Unit = {
    import org.ciroque.ccr.logging.CachingLogger
    logger.asInstanceOf[CachingLogger].reset()
  }

  override def beforeAll(): Unit = {
    settingsDataStore.upsertConfiguration(ConfigurationFactory(devEnvironment, application, loggingScope, logLevelSetting, "DEBUG", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(devEnvironment, application, loggingScope, logFilenameSetting, "output.log", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(devEnvironment, application, loggingScope, logRotationSetting, "24hours", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory(qaEnvironment, application, loggingScope, logLevelSetting, "DEBUG", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(qaEnvironment, application, loggingScope, logFilenameSetting, "output.log", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(qaEnvironment, application, loggingScope, logRotationSetting, "24hours", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application, loggingScope, logLevelSetting, "DEBUG", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application, loggingScope, logFilenameSetting, "output.log", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application, loggingScope, logRotationSetting, "24hours", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory(devEnvironment, application2, loggingScope, logLevelSetting, "DEBUG", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(devEnvironment, application2, loggingScope, logFilenameSetting, "output.log", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(devEnvironment, application2, loggingScope, logRotationSetting, "24hours", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory(qaEnvironment, application2, loggingScope, logLevelSetting, "DEBUG", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(qaEnvironment, application2, loggingScope, logFilenameSetting, "output.log", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(qaEnvironment, application2, loggingScope, logRotationSetting, "24hours", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application2, loggingScope, logLevelSetting, "DEBUG", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application2, loggingScope, logFilenameSetting, "output.log", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application2, loggingScope, logRotationSetting, "24hours", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    /* --- temporality --- */
    settingsDataStore.upsertConfiguration(activeLogLevelConfiguration)
    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application3, loggingScope, logFilenameSetting, "output.log", DateTime.now().minusYears(1), DateTime.now().plusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application3, loggingScope, logRotationSetting, "24hours", DateTime.now().minusYears(1), DateTime.now().plusDays(7), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application3, loggingScope, logLevelSetting, "DEBUG", DateTime.now().plusDays(8), DateTime.now().plusYears(1), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application3, loggingScope, logFilenameSetting, "output.log", DateTime.now().plusDays(7), DateTime.now().plusYears(1), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application3, loggingScope, logRotationSetting, "24hours", DateTime.now().plusDays(7), DateTime.now().plusYears(1), 360000L))

    settingsDataStore.upsertConfiguration(defaultLogRotationConfig)
    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, "app4", loggingScope, logRotationSetting, "24hours", DateTime.now().plusDays(7), DateTime.now().plusYears(1), 360000L))
  }

  private def assertLogEvents(name: String, count: Int, shouldInclude: String*) = {
    import org.ciroque.ccr.logging.CachingLogger
    val clog = logger.asInstanceOf[CachingLogger]
    clog.getEvents.size shouldBe count
    val logEvent = clog.getEvents.head
    (name +: shouldInclude).foreach(expected => logEvent should include(expected))
  }

  describe("configurations") {

    it("Upserts a valid configuration") {
      whenReady(settingsDataStore.upsertConfiguration(testConfiguration), Timeout(Span.Max)) {
        retrievedConfiguration =>11
          retrievedConfiguration should be(DataStoreResults.Added(testConfiguration))
      }

      assertLogEvents("upsertConfiguration", 1, "added-configuration")
    }

    it("Returns a single, active configuration when given a valid path") {
      whenReady(settingsDataStore.retrieveConfiguration(prodEnvironment, application3, loggingScope, logLevelSetting), Timeout(Span.Max)) {
        case Found(config) =>
          config match {
            case conf: List[Configuration] =>
              conf.size should be(1)
              conf.head should be(activeLogLevelConfiguration)
              conf.head.isActive should be(true)
            case something => fail(s"Expected to get a Configuration. Got a $something instead")
          }
        case NotFound(msg) => fail(s">>>>>>>>>>>>> $msg")
      }

      assertLogEvents("retrieveConfiguration", 1, prodEnvironment, application3, loggingScope, logLevelSetting)
    }

    it("Returns a NotFound when the environment / application / scope / setting combination does not exist") {
      whenReady(settingsDataStore.retrieveConfiguration(nonExistentSegment, nonExistentSegment, nonExistentSegment, nonExistentSegment), Timeout(Span.Max)) {
        result =>
          result should be(DataStoreResults.NotFound(s"environment '$nonExistentSegment' / application '$nonExistentSegment' / scope '$nonExistentSegment' / setting '$nonExistentSegment' combination was not found"))
      }

      assertLogEvents("retrieveConfiguration", 1, nonExistentSegment)
    }

    it("Returns a NotFound when there is no active configuration") {
      whenReady(settingsDataStore.retrieveConfiguration(devEnvironment, application, loggingScope, logLevelSetting), Timeout(Span.Max)) {
        result =>
          result should be(DataStoreResults.NotFound(s"environment '$devEnvironment' / application '$application' / scope '$loggingScope' / setting '$logLevelSetting' found no active configuration"))
      }

      assertLogEvents("retrieveConfiguration", 1, devEnvironment, application, loggingScope, logLevelSetting)
    }

    it("Returns a default, if present, when no active configuration is present") {
      whenReady(settingsDataStore.retrieveConfiguration(prodEnvironment, application4, loggingScope, logRotationSetting), Timeout(Span.Max)) {
        result =>
          result should be(DataStoreResults.Found(Seq(defaultLogRotationConfig)))
      }

      assertLogEvents("retrieveConfiguration", 1, prodEnvironment, application4, loggingScope, logRotationSetting)
    }
  }

  describe("environments") {
    it("Returns the correct environments") {
      whenReady(settingsDataStore.retrieveEnvironments(), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String]("default", devEnvironment, prodEnvironment, qaEnvironment, testEnvironment).sortBy(s => s)))
      }

      assertLogEvents("retrieveEnvironments", 1)
    }
  }

  describe("applications") {
    it("Returns the correct applications") {
      whenReady(settingsDataStore.retrieveApplications(devEnvironment), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String](application, application2).sortBy(s => s)))
      }

      assertLogEvents("retrieveApplications", 1, devEnvironment)
    }

    it("Returns a NotFound when the given environment does not exist") {
      whenReady(settingsDataStore.retrieveApplications(nonExistentSegment), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.NotFound(s"environment '$nonExistentSegment' was not found"))
      }

      assertLogEvents("retrieveApplications", 1, nonExistentSegment)
    }
  }

  describe("scopes") {
    it("Returns the correct scopes") {
      whenReady(settingsDataStore.retrieveScopes(devEnvironment, application), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String](loggingScope)))
      }

      assertLogEvents("retrieveScopes", 1, devEnvironment, application)
    }

    it("Returns a NotFound when the given environment / application combination does not exist") {
      whenReady(settingsDataStore.retrieveScopes(nonExistentSegment, nonExistentSegment), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.NotFound(s"environment '$nonExistentSegment' / application '$nonExistentSegment' combination was not found"))
      }

      assertLogEvents("retrieveScopes", 1, nonExistentSegment)
    }
  }

  describe("settings") {
    it("Returns the correct settings") {
      whenReady(settingsDataStore.retrieveSettings(devEnvironment, application, loggingScope), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String](logLevelSetting, logRotationSetting, logFilenameSetting).sortBy(s => s)))
      }

      assertLogEvents("retrieveSettings", 1, devEnvironment, application, loggingScope)
    }

    it("Returns a NotFound when the given environment / application combination does not exist") {
      whenReady(settingsDataStore.retrieveSettings(nonExistentSegment, nonExistentSegment, nonExistentSegment), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.NotFound(s"environment '$nonExistentSegment' / application '$nonExistentSegment' / scope '$nonExistentSegment' combination was not found"))
      }

      assertLogEvents("retrieveSettings", 1, nonExistentSegment)
    }
  }
}
