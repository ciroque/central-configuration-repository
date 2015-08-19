package org.ciroque.ccr.datastores

import org.ciroque.ccr.models.ConfigurationFactory
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, BeforeAndAfter, FunSpec, Matchers}

class InMemorySettingsDataStoreTests
  extends FunSpec
  with Matchers
  with ScalaFutures
  with BeforeAndAfterEach {

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

  val settingsDataStore = new InMemorySettingsDataStore()

  override def beforeEach(): Unit = {
    super.beforeEach()

    settingsDataStore.upsertConfiguration(ConfigurationFactory("dev", "app", "scope", "loglevel", "DEBUG", DateTime.now().minusYears(1), DateTime.now(), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("dev", "app", "scope", "logfilename", "output.log", DateTime.now().minusYears(1), DateTime.now(), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("dev", "app", "scope", "logrotation", "24hours", DateTime.now().minusYears(1), DateTime.now(), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory("qa", "app", "scope", "loglevel", "DEBUG", DateTime.now().minusYears(1), DateTime.now(), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("qa", "app", "scope", "logfilename", "output.log", DateTime.now().minusYears(1), DateTime.now(), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("qa", "app", "scope", "logrotation", "24hours", DateTime.now().minusYears(1), DateTime.now(), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory("prod", "app", "scope", "loglevel", "DEBUG", DateTime.now().minusYears(1), DateTime.now(), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("prod", "app", "scope", "logfilename", "output.log", DateTime.now().minusYears(1), DateTime.now(), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("prod", "app", "scope", "logrotation", "24hours", DateTime.now().minusYears(1), DateTime.now(), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory("dev", "app2", "scope", "loglevel", "DEBUG", DateTime.now().minusYears(1), DateTime.now(), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("dev", "app2", "scope", "logfilename", "output.log", DateTime.now().minusYears(1), DateTime.now(), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("dev", "app2", "scope", "logrotation", "24hours", DateTime.now().minusYears(1), DateTime.now(), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory("qa", "app2", "scope", "loglevel", "DEBUG", DateTime.now().minusYears(1), DateTime.now(), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("qa", "app2", "scope", "logfilename", "output.log", DateTime.now().minusYears(1), DateTime.now(), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("qa", "app2", "scope", "logrotation", "24hours", DateTime.now().minusYears(1), DateTime.now(), 360000L))

    settingsDataStore.upsertConfiguration(ConfigurationFactory("prod", "app2", "scope", "loglevel", "DEBUG", DateTime.now().minusYears(1), DateTime.now(), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("prod", "app2", "scope", "logfilename", "output.log", DateTime.now().minusYears(1), DateTime.now(), 360000L))
    settingsDataStore.upsertConfiguration(ConfigurationFactory("prod", "app2", "scope", "logrotation", "24hours", DateTime.now().minusYears(1), DateTime.now(), 360000L))
  }

  describe("InMemorySettingsDataStore") {
    describe("configurations") {
      it("Upserts a valid configuration") {
        whenReady(settingsDataStore.upsertConfiguration(testConfiguration)) {
          retrievedConfiguration =>
            retrievedConfiguration should be(DataStoreResults.Added(testConfiguration))
        }
      }

      it("Returns a NotFound when the environment / application / scope / setting combination does not exist") {
        whenReady(settingsDataStore.retrieveConfiguration(nonExistentSegment, nonExistentSegment, nonExistentSegment, nonExistentSegment)) {
          result =>
            result should be(DataStoreResults.NotFound(s"environment '$nonExistentSegment' / application '$nonExistentSegment' / scope '$nonExistentSegment' / setting '$nonExistentSegment' combination was not found"))
        }
      }
    }

    describe("environments") {
      it("Returns the correct environments") {
        whenReady(settingsDataStore.retrieveEnvironments()) { result =>
          result should be(DataStoreResults.Found(List[String]("dev", "prod", "qa", testEnvironment).sortBy(s => s)))
        }
      }
    }

    describe("applications") {
      it("Returns the correct applications") {
        whenReady(settingsDataStore.retrieveApplications("dev")) { result =>
          result should be(DataStoreResults.Found(List[String]("app", "app2").sortBy(s => s)))
        }
      }

      it("Returns a NotFound when the given environment does not exist") {
        whenReady(settingsDataStore.retrieveApplications(nonExistentSegment)) { result =>
          result should be(DataStoreResults.NotFound(s"environment '$nonExistentSegment' was not found"))
        }
      }
    }

    describe("scopes") {
      it("Returns the correct scopes") {
        whenReady(settingsDataStore.retrieveScopes("dev", "app")) { result =>
          result should be(DataStoreResults.Found(List[String]("scope")))
        }
      }

      it("Returns a NotFound when the given environment / application combination does not exist") {
        whenReady(settingsDataStore.retrieveScopes(nonExistentSegment, nonExistentSegment)) { result =>
          result should be(DataStoreResults.NotFound(s"environment '$nonExistentSegment' / application '$nonExistentSegment' combination was not found"))
        }
      }
    }

    describe("settings") {
      it("Returns the correct settings") {
        whenReady(settingsDataStore.retrieveSettings("dev", "app", "scope")) { result =>
          result should be(DataStoreResults.Found(List[String]("loglevel", "logfilename", "logrotation").sortBy(s => s)))
        }
      }

      it("Returns a NotFound when the given environment / application combination does not exist") {
        whenReady(settingsDataStore.retrieveSettings(nonExistentSegment, nonExistentSegment, nonExistentSegment)) { result =>
          result should be(DataStoreResults.NotFound(s"environment '$nonExistentSegment' / application '$nonExistentSegment' / scope '$nonExistentSegment' combination was not found"))
        }
      }
    }
  }
}
