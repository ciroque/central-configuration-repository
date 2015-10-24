package org.ciroque.ccr.datastores

import java.util.UUID

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
  val testValue = Left("test-value")
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

  val wildcardEnvironment: String = "environment:wildcard"
  val wildcardApplication: String = "application:wildcard"
  val wildcardScope: String = "scope:wildcard"
  val wildcardSetting: String = "setting:wildcard"

  val nonExistentSegment: String = "non-existent"

  val sourceIdEnvironment = "env:sourceid"
  val uniqueEnvironment = "UNIQ:Environment"
  val sourceId = UUID.randomUUID()
  val secondSourceId = UUID.randomUUID().toString

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

  val activeLogLevelConfiguration: Configuration = ConfigurationFactory(prodEnvironment, application3, loggingScope, logLevelSetting, Left("BING"), DateTime.now().minusYears(1), DateTime.now().plusDays(7), 360000L)
  val defaultLogRotationConfiguration: Configuration = ConfigurationFactory("default", "app4", loggingScope, logRotationSetting, Left("12hours"), DateTime.now().plusDays(7), DateTime.now().plusYears(1), 360000L)
  val wildcardConfiguration: Configuration = ConfigurationFactory(wildcardEnvironment, wildcardApplication, wildcardScope, wildcardSetting, Left("WILDCARD"), DateTime.now().minusDays(2), DateTime.now().plusDays(2), 360000L)
  val ting = Right(Map("KEY" → "value"))
  val configurationWithSourceId: Configuration = ConfigurationFactory(
    UUID.randomUUID(),
    sourceIdEnvironment,
    "app:sourceid",
    "scp:sourceid",
    "stg:sourceid",
    Some(sourceId.toString),
    ting,
    DateTime.now().minusDays(7),
    DateTime.now().plusDays(7),
    5 * 60 * 1000L)

  val alternateConfigurationWithSourceId = configurationWithSourceId.copy(_id = UUID.randomUUID(), key = configurationWithSourceId.key.copy(sourceId = Some(secondSourceId)), value = Left("SOMETHING_DIFFERENT"))

  override def beforeEach(): Unit = {
    import org.ciroque.ccr.logging.CachingLogger
    logger.asInstanceOf[CachingLogger].reset()
  }

  override def beforeAll(): Unit = {
    settingsDataStore.upsertConfiguration(ConfigurationFactory(devEnvironment, application, loggingScope, logLevelSetting, Left("DEBUG"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(devEnvironment, application, loggingScope, logFilenameSetting, Left("output.log"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(devEnvironment, application, loggingScope, logRotationSetting, Left("24hours"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory(qaEnvironment, application, loggingScope, logLevelSetting, Left("DEBUG"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(qaEnvironment, application, loggingScope, logFilenameSetting, Left("output.log"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(qaEnvironment, application, loggingScope, logRotationSetting, Left("24hours"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application, loggingScope, logLevelSetting, Left("DEBUG"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application, loggingScope, logFilenameSetting, Left("output.log"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application, loggingScope, logRotationSetting, Left("24hours"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory(devEnvironment, application2, loggingScope, logLevelSetting, Left("DEBUG"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(devEnvironment, application2, loggingScope, logFilenameSetting, Left("output.log"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(devEnvironment, application2, loggingScope, logRotationSetting, Left("24hours"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory(qaEnvironment, application2, loggingScope, logLevelSetting, Left("DEBUG"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(qaEnvironment, application2, loggingScope, logFilenameSetting, Left("output.log"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(qaEnvironment, application2, loggingScope, logRotationSetting, Left("24hours"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application2, loggingScope, logLevelSetting, Left("DEBUG"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application2, loggingScope, logFilenameSetting, Left("output.log"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application2, loggingScope, logRotationSetting, Left("24hours"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    /* --- temporality --- */
    settingsDataStore.upsertConfiguration(activeLogLevelConfiguration)
    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application3, loggingScope, logFilenameSetting, Left("output.log"), DateTime.now().minusYears(1), DateTime.now().plusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application3, loggingScope, logRotationSetting, Left("24hours"), DateTime.now().minusYears(1), DateTime.now().plusDays(7), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application3, loggingScope, logLevelSetting, Left("DEBUG"), DateTime.now().plusDays(8), DateTime.now().plusYears(1), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application3, loggingScope, logFilenameSetting, Left("output.log"), DateTime.now().plusDays(7), DateTime.now().plusYears(1), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, application3, loggingScope, logRotationSetting, Left("24hours"), DateTime.now().plusDays(7), DateTime.now().plusYears(1), 360000L))

    settingsDataStore.upsertConfiguration(defaultLogRotationConfiguration)
    settingsDataStore.upsertConfiguration(ConfigurationFactory(prodEnvironment, "app4", loggingScope, logRotationSetting, Left("24hours"), DateTime.now().plusDays(7), DateTime.now().plusYears(1), 360000L))

    settingsDataStore.upsertConfiguration(wildcardConfiguration)

    settingsDataStore.upsertConfiguration(configurationWithSourceId)
    settingsDataStore.upsertConfiguration(alternateConfigurationWithSourceId)
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
        retrievedConfiguration =>
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
        case NotFound(msg) => fail(s"NotFound -> $msg")
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
          result should be(DataStoreResults.Found(Seq(defaultLogRotationConfiguration)))
      }

      assertLogEvents("retrieveConfiguration", 1, prodEnvironment, application4, loggingScope, logRotationSetting)
    }

    it("returns a setting with leading wildcards in the environment, applications, scope, and settings segments") {
      val leadingWildcardSearch = "*:wildcard"
      whenReady(settingsDataStore.retrieveConfiguration(leadingWildcardSearch, leadingWildcardSearch, leadingWildcardSearch, leadingWildcardSearch), Timeout(Span.Max)) {
        result =>
          result should be(DataStoreResults.Found(Seq(wildcardConfiguration)))
      }

      assertLogEvents("retrieveConfiguration", 1, leadingWildcardSearch)
    }

    it("Allows inclusion of a sourceKey value in the key field") {
      val configurationWithSourceId = ConfigurationFactory(
        UUID.randomUUID(),
        uniqueEnvironment,
        "UNIQ:Application",
        "UNIQ:Scope",
        "UNIQ:Setting",
        Some("UNIQ:SourceId"),
        Left("UNIQUE_VALUE"),
        DateTime.now().minusDays(2),
        DateTime.now().plusDays(2),
        10L
      )

      whenReady(settingsDataStore.upsertConfiguration(configurationWithSourceId), Timeout(Span.Max)) {
        retrievedConfiguration =>
          retrievedConfiguration should be(DataStoreResults.Added(configurationWithSourceId))
      }
    }

    it("restricts the sourceId to 64 characters") {
      val uniqueApplication: String = "UNIQ:Application"
      val uniqueScope: String = "UNIQ:Scope"
      val uniqueSetting: String = "UNIQ:Setting"
      val sourceId = "1234567890123456789012345678901234567890123456789012345678901234567890"
      val configurationWithSourceId = ConfigurationFactory(
        UUID.randomUUID(),
        uniqueEnvironment,
        uniqueApplication,
        uniqueScope,
        uniqueSetting,
        Some(sourceId),
        Right(Map("KEY" -> "UNIQUE_VALUE")),
        DateTime.now().minusDays(2),
        DateTime.now().plusDays(2),
        10L
      )

      val expectedSourceId = "1234567890123456789012345678901234567890123456789012345678901234"
      val expectedKey = ConfigurationFactory.Key(uniqueEnvironment, uniqueApplication, uniqueScope, uniqueSetting, Some(expectedSourceId))
      val expectedConfiguration = configurationWithSourceId.copy(key = expectedKey)

      whenReady(settingsDataStore.upsertConfiguration(configurationWithSourceId), Timeout(Span.Max)) {
        retrievedConfiguration =>
          retrievedConfiguration should be(DataStoreResults.Added(expectedConfiguration))
      }

      assertLogEvents("upsertConfiguration", 1, "given-configuration", "added-configuration", sourceId, expectedSourceId)
    }

    it("allows filtering by the sourceId") {
      whenReady(
        settingsDataStore.retrieveConfiguration(
          configurationWithSourceId.key.environment,
          configurationWithSourceId.key.application,
          configurationWithSourceId.key.scope,
          configurationWithSourceId.key.setting,
          Some(sourceId.toString)),
        Timeout(Span.Max)) {

        case Found(config) =>
          config match {
            case conf: List[Configuration] =>
              conf.size should be(1)
              conf.head should be(configurationWithSourceId)
              conf.head.isActive should be(true)
            case something => fail(s"Expected to get a Configuration. Got a $something instead.")
          }
        case NotFound(msg) => fail(s"NotFound -> $msg")
      }
    }

    it("returns all the configurations when the sourceId is not matched") {
      whenReady(
        settingsDataStore.retrieveConfiguration(
          configurationWithSourceId.key.environment,
          configurationWithSourceId.key.application,
          configurationWithSourceId.key.scope,
          configurationWithSourceId.key.setting,
          Some("NOT_THERE")),
        Timeout(Span.Max)) {

        case Found(config) =>
          config match {
            case conf: List[Configuration] =>
              conf.size should be(2)
              conf.contains(configurationWithSourceId)
              conf.contains(alternateConfigurationWithSourceId)
            case something => fail(s"Expected to get a Configuration. Got a $something instead.")
          }
        case NotFound(msg) => fail(s"NotFound -> $msg")
      }
    }
  }

  describe("environments") {
    it("Returns the correct environments") {
      whenReady(settingsDataStore.retrieveEnvironments(), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String](uniqueEnvironment, "default", devEnvironment, sourceIdEnvironment, wildcardEnvironment, prodEnvironment, qaEnvironment, testEnvironment).sortBy(s => s)))
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

    it("Handles wildcard searches for environments") {
      val wildcardQuery = "environment:*"
      whenReady(settingsDataStore.retrieveApplications(wildcardQuery), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String](wildcardApplication)))
      }

      assertLogEvents("retrieveApplications", 1, wildcardQuery)
    }

    it("Handles regex wildcard searches for environments") {
      val wildcardQuery = "environment:.*"
      whenReady(settingsDataStore.retrieveApplications(wildcardQuery), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String](wildcardApplication)))
      }

      assertLogEvents("retrieveApplications", 1, wildcardQuery)
    }

    it("Handles trailing wildcard searches for environments") {
      val wildcardQuery = "environment*"
      whenReady(settingsDataStore.retrieveApplications(wildcardQuery), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String](wildcardApplication, testApplication)))
      }

      assertLogEvents("retrieveApplications", 1, wildcardQuery)
    }

    it("Handles leading wildcard searches for environments") {
      val wildcardQuery = "*:wildcard"
      whenReady(settingsDataStore.retrieveApplications(wildcardQuery), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String](wildcardApplication)))
      }

      assertLogEvents("retrieveApplications", 1, wildcardQuery)
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

    it("Finds a scope given a leading wildcard in the environment") {
      val wildcardEnvironmentSearch = "*:wildcard"
      whenReady(settingsDataStore.retrieveScopes(wildcardEnvironmentSearch, wildcardApplication), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String](wildcardScope)))
      }

      assertLogEvents("retrieveScopes", 1, wildcardEnvironmentSearch, wildcardApplication)
    }

    it("Finds a scope given a leading wildcard in the environment and application") {
      val wildcardSearch = "*:wildcard"
      whenReady(settingsDataStore.retrieveScopes(wildcardSearch, wildcardSearch), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String](wildcardScope)))
      }

      assertLogEvents("retrieveScopes", 1, wildcardSearch)
    }

    it("Finds a scope given a trailing wildcard in the environment") {
      val wildcardEnvironmentSearch = "environment:*"
      whenReady(settingsDataStore.retrieveScopes(wildcardEnvironmentSearch, wildcardApplication), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String](wildcardScope)))
      }

      assertLogEvents("retrieveScopes", 1, wildcardEnvironmentSearch, wildcardApplication)
    }

    it("Finds a scope given a trailing wildcard in the environment and application") {
      val wildcardEnvironmentSearch = "environment:*"
      val wildcardApplicationSearch = "application:*"
      whenReady(settingsDataStore.retrieveScopes(wildcardEnvironmentSearch, wildcardApplicationSearch), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String](wildcardScope)))
      }

      assertLogEvents("retrieveScopes", 1, wildcardEnvironmentSearch, wildcardApplicationSearch)
    }

    it("Finds a scope given a embedded wildcard in the environment and application") {
      val wildcardEnvironmentSearch = "environ*wildcard"
      val wildcardApplicationSearch = "app*card"
      whenReady(settingsDataStore.retrieveScopes(wildcardEnvironmentSearch, wildcardApplicationSearch), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String](wildcardScope)))
      }

      assertLogEvents("retrieveScopes", 1, wildcardEnvironmentSearch, wildcardApplicationSearch)
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

    it("Finds a setting given a leading wildcard in the environment") {
      val wildcardEnvironmentSearch = "*:wildcard"
      whenReady(settingsDataStore.retrieveSettings(wildcardEnvironmentSearch, wildcardApplication, wildcardScope), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String](wildcardSetting)))
      }

      assertLogEvents("retrieveSettings", 1, wildcardEnvironmentSearch, wildcardApplication, wildcardScope)
    }

    it("Finds a setting given a leading wildcard in the environment and application") {
      val wildcardSearch = "*:wildcard"
      whenReady(settingsDataStore.retrieveSettings(wildcardSearch, wildcardSearch, wildcardScope), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String](wildcardSetting)))
      }

      assertLogEvents("retrieveSettings", 1, wildcardSearch, wildcardScope)
    }

    it("Finds a setting given a trailing wildcard in the environment") {
      val wildcardEnvironmentSearch = "environment:*"
      whenReady(settingsDataStore.retrieveSettings(wildcardEnvironmentSearch, wildcardApplication, wildcardScope), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String](wildcardSetting)))
      }

      assertLogEvents("retrieveSettings", 1, wildcardEnvironmentSearch, wildcardApplication, wildcardScope)
    }

    it("Finds a setting given a trailing wildcard in the environment and application") {
      val wildcardEnvironmentSearch = "environment:*"
      val wildcardApplicationSearch = "application:*"
      whenReady(settingsDataStore.retrieveSettings(wildcardEnvironmentSearch, wildcardApplicationSearch, wildcardScope), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String](wildcardSetting)))
      }

      assertLogEvents("retrieveSettings", 1, wildcardEnvironmentSearch, wildcardApplicationSearch, wildcardScope)
    }

    it("Finds a setting given a trailing wildcard in the environment, application, and scope") {
      val wildcardEnvironmentSearch = "environment:*"
      val wildcardApplicationSearch = "application:*"
      val wildcardScopeSearch = "scope:*"
      whenReady(settingsDataStore.retrieveSettings(wildcardEnvironmentSearch, wildcardApplicationSearch, wildcardScopeSearch), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String](wildcardSetting)))
      }

      assertLogEvents("retrieveSettings", 1, wildcardEnvironmentSearch, wildcardApplicationSearch, wildcardScopeSearch)
    }

    it("Finds a setting given a embedded wildcard in the environment, application, and scope") {
      val wildcardEnvironmentSearch = "environ*wildcard"
      val wildcardApplicationSearch = "app*card"
      val wildcardScopeSearch = "sco*card"
      whenReady(settingsDataStore.retrieveSettings(wildcardEnvironmentSearch, wildcardApplicationSearch, wildcardScopeSearch), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String](wildcardSetting)))
      }

      assertLogEvents("retrieveSettings", 1, wildcardEnvironmentSearch, wildcardApplicationSearch, wildcardScopeSearch)
    }
  }
}
