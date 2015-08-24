package org.ciroque.ccr.datastores

import org.ciroque.ccr.datastores.DataStoreResults.{NotFound, Found}
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.models.ConfigurationFactory.Configuration
import org.joda.time.DateTime
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.Span
import org.scalatest.{BeforeAndAfterAll, Matchers, FunSpec}

abstract class SettingsDataStoreTests
  extends FunSpec
  with Matchers
  with ScalaFutures
  with BeforeAndAfterAll {

  implicit val settingsDataStore: SettingsDataStore

  val testEnvironment = "test-environment"
  val testApplication = "test-application"
  val testScope = "test-scope"
  val testSetting = "test-settings"
  val testValue = "test-value"
  val testEffectiveAt = DateTime.now().minusMonths(1)
  val testExpiresAt = DateTime.now().plusMonths(1)
  val ttl = 360000

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

  val activeLogLevelConfiguration: Configuration = ConfigurationFactory("prod", "app3", "logging", "loglevel", "ALL", DateTime.now().minusYears(1), DateTime.now().plusDays(7), 360000L)
  val defaultLogRotationConfig: Configuration = ConfigurationFactory("default", "app4", "logging", "logrotation", "12hours", DateTime.now().plusDays(7), DateTime.now().plusYears(1), 360000L)

  override def beforeAll(): Unit = {
//    super.beforeAll()

    settingsDataStore.upsertConfiguration(ConfigurationFactory("dev", "app", "scope", "loglevel", "DEBUG", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("dev", "app", "scope", "logfilename", "output.log", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("dev", "app", "scope", "logrotation", "24hours", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory("qa", "app", "scope", "loglevel", "DEBUG", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("qa", "app", "scope", "logfilename", "output.log", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("qa", "app", "scope", "logrotation", "24hours", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory("prod", "app", "scope", "loglevel", "DEBUG", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("prod", "app", "scope", "logfilename", "output.log", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("prod", "app", "scope", "logrotation", "24hours", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory("dev", "app2", "scope", "loglevel", "DEBUG", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("dev", "app2", "scope", "logfilename", "output.log", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("dev", "app2", "scope", "logrotation", "24hours", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory("qa", "app2", "scope", "loglevel", "DEBUG", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("qa", "app2", "scope", "logfilename", "output.log", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("qa", "app2", "scope", "logrotation", "24hours", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory("prod", "app2", "scope", "loglevel", "DEBUG", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("prod", "app2", "scope", "logfilename", "output.log", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("prod", "app2", "scope", "logrotation", "24hours", DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    /* --- temporality --- */
    settingsDataStore.upsertConfiguration(activeLogLevelConfiguration)
    settingsDataStore.upsertConfiguration(ConfigurationFactory("prod", "app3", "logging", "logfilename", "output.log", DateTime.now().minusYears(1), DateTime.now().plusDays(7), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("prod", "app3", "logging", "logrotation", "24hours", DateTime.now().minusYears(1), DateTime.now().plusDays(7), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory("prod", "app3", "logging", "loglevel", "DEBUG", DateTime.now().plusDays(8), DateTime.now().plusYears(1), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("prod", "app3", "logging", "logfilename", "output.log", DateTime.now().plusDays(7), DateTime.now().plusYears(1), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("prod", "app3", "logging", "logrotation", "24hours", DateTime.now().plusDays(7), DateTime.now().plusYears(1), 360000L))

    settingsDataStore.upsertConfiguration(defaultLogRotationConfig)
    settingsDataStore.upsertConfiguration(ConfigurationFactory("prod", "app4", "logging", "logrotation", "24hours", DateTime.now().plusDays(7), DateTime.now().plusYears(1), 360000L))
  }

  describe("configurations") {
    it("Upserts a valid configuration") {
      whenReady(settingsDataStore.upsertConfiguration(testConfiguration), Timeout(Span.Max)) {
        retrievedConfiguration =>
          retrievedConfiguration should be(DataStoreResults.Added(testConfiguration))
      }
    }

    it("Returns a single, active configuration when given a valid path") {
      whenReady(settingsDataStore.retrieveConfiguration("prod", "app3", "logging", "loglevel"), Timeout(Span.Max)) {
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
    }

    it("Returns a NotFound when the environment / application / scope / setting combination does not exist") {
      whenReady(settingsDataStore.retrieveConfiguration(nonExistentSegment, nonExistentSegment, nonExistentSegment, nonExistentSegment), Timeout(Span.Max)) {
        result =>
          result should be(DataStoreResults.NotFound(s"environment '$nonExistentSegment' / application '$nonExistentSegment' / scope '$nonExistentSegment' / setting '$nonExistentSegment' combination was not found"))
      }
    }

    it("Returns a NotFound when there is no active configuration") {
      whenReady(settingsDataStore.retrieveConfiguration("dev", "app", "scope", "loglevel"), Timeout(Span.Max)) {
        result =>
          result should be(DataStoreResults.NotFound(s"environment 'dev' / application 'app' / scope 'scope' / setting 'loglevel' found no active configuration"))
      }
    }

    it("Returns a default, if present, when no active configuration is present") {
      whenReady(settingsDataStore.retrieveConfiguration("prod", "app4", "logging", "logrotation"), Timeout(Span.Max)) {
        result =>
          result should be(DataStoreResults.Found(Seq(defaultLogRotationConfig)))
      }
    }
  }

  describe("environments") {
    it("Returns the correct environments") {
      whenReady(settingsDataStore.retrieveEnvironments(), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String]("default", "dev", "prod", "qa", testEnvironment).sortBy(s => s)))
      }
    }
  }

  describe("applications") {
    it("Returns the correct applications") {
      whenReady(settingsDataStore.retrieveApplications("dev"), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String]("app", "app2").sortBy(s => s)))
      }
    }

    it("Returns a NotFound when the given environment does not exist") {
      whenReady(settingsDataStore.retrieveApplications(nonExistentSegment), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.NotFound(s"environment '$nonExistentSegment' was not found"))
      }
    }
  }

  describe("scopes") {
    it("Returns the correct scopes") {
      whenReady(settingsDataStore.retrieveScopes("dev", "app"), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String]("scope")))
      }
    }

    it("Returns a NotFound when the given environment / application combination does not exist") {
      whenReady(settingsDataStore.retrieveScopes(nonExistentSegment, nonExistentSegment), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.NotFound(s"environment '$nonExistentSegment' / application '$nonExistentSegment' combination was not found"))
      }
    }
  }

  describe("settings") {
    it("Returns the correct settings") {
      whenReady(settingsDataStore.retrieveSettings("dev", "app", "scope"), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.Found(List[String]("loglevel", "logfilename", "logrotation").sortBy(s => s)))
      }
    }

    it("Returns a NotFound when the given environment / application combination does not exist") {
      whenReady(settingsDataStore.retrieveSettings(nonExistentSegment, nonExistentSegment, nonExistentSegment), Timeout(Span.Max)) { result =>
        result should be(DataStoreResults.NotFound(s"environment '$nonExistentSegment' / application '$nonExistentSegment' / scope '$nonExistentSegment' combination was not found"))
      }
    }
  }
}
