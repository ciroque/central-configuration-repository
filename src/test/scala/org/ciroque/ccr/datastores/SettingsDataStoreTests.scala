package org.ciroque.ccr.datastores

import java.util.UUID

import org.ciroque.ccr.core.Commons
import org.ciroque.ccr.datastores.DataStoreResults._
import org.ciroque.ccr.helpers.TestObjectGenerator
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.models.ConfigurationFactory._
import org.joda.time.DateTime
import org.scalatest._
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.Span
import spray.json._

abstract class SettingsDataStoreTests
  extends FunSpec
  with Matchers
  with ScalaFutures
  with BeforeAndAfterAll
  with BeforeAndAfterEach {

  import org.slf4j.Logger
  import spray.json.{JsObject, JsString}

  implicit val settingsDataStore: SettingsDataStore
  implicit val logger: Logger

  val testEnvironment = "test-environment"
  val testApplication = "test-application"
  val testScope = "test-scope"
  val testSetting = "test-settings"
  val testValue = JsObject(
    "test-key" → JsString("test-value"),
    "nested" → JsObject("nested-key" → JsString("nested-value"))
    , "a_number" → JsNumber(1440)
    , "a_boolean" → JsBoolean(true)
    , "a_null" → JsNull
    , "an_array" → JsArray(Vector(JsString("One"), JsNumber(3.3), JsNumber(2), JsObject("four" → JsNumber(4))))
  )
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

  val jsonValue = JsObject(Map("KEY" → JsString("value")))
  val activeLogLevelConfiguration: Configuration = ConfigurationFactory(prodEnvironment, application3, loggingScope, logLevelSetting, JsString("BING"), DateTime.now().minusYears(1), DateTime.now().plusDays(7), 360000L)
  val defaultLogRotationConfiguration: Configuration = ConfigurationFactory("default", "app4", loggingScope, logRotationSetting, JsString("12hours"), DateTime.now().plusDays(7), DateTime.now().plusYears(1), 360000L)
  val wildcardConfiguration: Configuration = ConfigurationFactory(wildcardEnvironment, wildcardApplication, wildcardScope, wildcardSetting, JsString("WILDCARD"), DateTime.now().minusDays(2), DateTime.now().plusDays(2), 360000L)
  val configurationWithSourceId: Configuration = ConfigurationFactory(
    UUID.randomUUID(),
    sourceIdEnvironment,
    "app:sourceid",
    "scp:sourceid",
    "stg:sourceid",
    Some(sourceId.toString),
    jsonValue,
    DateTime.now().minusDays(7),
    DateTime.now().plusDays(7),
    5 * 60 * 1000L)

  val alternateConfigurationWithSourceId = configurationWithSourceId.copy(_id = UUID.randomUUID(), key = configurationWithSourceId.key.copy(sourceId = Some(secondSourceId)), value = JsString("SOMETHING_DIFFERENT"))

  val app = "THIS"
  val env = "IS"
  val scp1 = "A"
  val scp2 = "AN"
  val scp3 = "YETANOTHER"
  val scpSched = "SCHEDULED"
  val stg = "BULK"
  val similarOne = TestObjectGenerator.configuration(app, env, scp1, stg)
  val similarTwo = TestObjectGenerator.configuration(app, env, scp2, stg)
  val similarThree = TestObjectGenerator.configuration(app, env, scp3, stg)

  val now = DateTime.now
  val pastEffective = now.minusYears(1)
  val pastExpires = now.minusWeeks(1)

  val currentEffective = now.minusWeeks(1).plusSeconds(1)
  val currentExpires = now.plusMonths(1)

  val futureEffective = now.plusMonths(1).plusSeconds(1)
  val futureExpires = now.plusYears(1)

  val scheduled1 = TestObjectGenerator.configuration(app, env, scpSched, stg, futureEffective, futureExpires)
  val scheduled2 = TestObjectGenerator.configuration(app, env, scpSched, stg, pastEffective, pastExpires)
  val scheduled3 = TestObjectGenerator.configuration(app, env, scpSched, stg, currentEffective, currentExpires)
  val scheduled4 = TestObjectGenerator.configuration(ConfigurationFactory.DefaultEnvironment, env, scpSched, stg, pastEffective, futureExpires)

  override def beforeEach(): Unit = {
    import org.ciroque.ccr.logging.CachingLogger
    logger.asInstanceOf[CachingLogger].reset()
  }

  override def beforeAll(): Unit = {
    settingsDataStore.insertConfiguration(similarOne)
    settingsDataStore.insertConfiguration(similarTwo)
    settingsDataStore.insertConfiguration(similarThree)

    settingsDataStore.insertConfiguration(ConfigurationFactory(devEnvironment, application, loggingScope, logLevelSetting, JsString("DEBUG"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.insertConfiguration(ConfigurationFactory(devEnvironment, application, loggingScope, logFilenameSetting, JsString("output.log"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.insertConfiguration(ConfigurationFactory(devEnvironment, application, loggingScope, logRotationSetting, JsString("24hours"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    settingsDataStore.insertConfiguration(ConfigurationFactory(qaEnvironment, application, loggingScope, logLevelSetting, JsString("DEBUG"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.insertConfiguration(ConfigurationFactory(qaEnvironment, application, loggingScope, logFilenameSetting, JsString("output.log"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.insertConfiguration(ConfigurationFactory(qaEnvironment, application, loggingScope, logRotationSetting, JsString("24hours"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    settingsDataStore.insertConfiguration(ConfigurationFactory(prodEnvironment, application, loggingScope, logLevelSetting, JsString("DEBUG"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.insertConfiguration(ConfigurationFactory(prodEnvironment, application, loggingScope, logFilenameSetting, JsString("output.log"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.insertConfiguration(ConfigurationFactory(prodEnvironment, application, loggingScope, logRotationSetting, JsString("24hours"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    settingsDataStore.insertConfiguration(ConfigurationFactory(devEnvironment, application2, loggingScope, logLevelSetting, JsString("DEBUG"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.insertConfiguration(ConfigurationFactory(devEnvironment, application2, loggingScope, logFilenameSetting, JsString("output.log"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.insertConfiguration(ConfigurationFactory(devEnvironment, application2, loggingScope, logRotationSetting, JsString("24hours"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    settingsDataStore.insertConfiguration(ConfigurationFactory(qaEnvironment, application2, loggingScope, logLevelSetting, JsString("DEBUG"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.insertConfiguration(ConfigurationFactory(qaEnvironment, application2, loggingScope, logFilenameSetting, JsString("output.log"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.insertConfiguration(ConfigurationFactory(qaEnvironment, application2, loggingScope, logRotationSetting, JsString("24hours"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    settingsDataStore.insertConfiguration(ConfigurationFactory(prodEnvironment, application2, loggingScope, logLevelSetting, JsString("DEBUG"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.insertConfiguration(ConfigurationFactory(prodEnvironment, application2, loggingScope, logFilenameSetting, JsString("output.log"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))
    settingsDataStore.insertConfiguration(ConfigurationFactory(prodEnvironment, application2, loggingScope, logRotationSetting, JsString("24hours"), DateTime.now().minusYears(1), DateTime.now().minusDays(7), 360000L))

    /* --- temporality --- */
    settingsDataStore.insertConfiguration(activeLogLevelConfiguration)
    settingsDataStore.insertConfiguration(ConfigurationFactory(prodEnvironment, application3, loggingScope, logFilenameSetting, JsString("output.log"), DateTime.now().minusYears(1), DateTime.now().plusDays(7), 360000L))
    settingsDataStore.insertConfiguration(ConfigurationFactory(prodEnvironment, application3, loggingScope, logRotationSetting, JsString("24hours"), DateTime.now().minusYears(1), DateTime.now().plusDays(7), 360000L))

    settingsDataStore.insertConfiguration(ConfigurationFactory(prodEnvironment, application3, loggingScope, logLevelSetting, JsString("DEBUG"), DateTime.now().plusDays(8), DateTime.now().plusYears(1), 360000L))
    settingsDataStore.insertConfiguration(ConfigurationFactory(prodEnvironment, application3, loggingScope, logFilenameSetting, JsString("output.log"), DateTime.now().plusDays(7), DateTime.now().plusYears(1), 360000L))
    settingsDataStore.insertConfiguration(ConfigurationFactory(prodEnvironment, application3, loggingScope, logRotationSetting, JsString("24hours"), DateTime.now().plusDays(7), DateTime.now().plusYears(1), 360000L))

    settingsDataStore.insertConfiguration(defaultLogRotationConfiguration)
    settingsDataStore.insertConfiguration(ConfigurationFactory(prodEnvironment, "app4", loggingScope, logRotationSetting, JsString("24hours"), DateTime.now().plusDays(7), DateTime.now().plusYears(1), 360000L))

    settingsDataStore.insertConfiguration(wildcardConfiguration)

    settingsDataStore.insertConfiguration(configurationWithSourceId)
    settingsDataStore.insertConfiguration(alternateConfigurationWithSourceId)

    settingsDataStore.bulkInsertConfigurations(ConfigurationList(List(scheduled1, scheduled2, scheduled3, scheduled4)))
  }

  private def assertLogEvents(name: String, count: Int, shouldInclude: String*) = {
    import org.ciroque.ccr.logging.CachingLogger
    val clog = logger.asInstanceOf[CachingLogger]
    clog.getEvents.size shouldBe count
    val logEvent = clog.getEvents.head
    (name +: shouldInclude).foreach(expected => logEvent should include(expected))
  }

  describe("configurations") {

    describe("insert") {

      it("Inserts a valid configuration") {
        whenReady(settingsDataStore.insertConfiguration(testConfiguration), Timeout(Span.Max)) {
          dsr => dsr should be(DataStoreResults.Added(testConfiguration))
        }

        assertLogEvents("insertConfiguration", 1, "added-configuration")
      }

      it("Fails to insert an existing configuration") {
        whenReady(settingsDataStore.insertConfiguration(testConfiguration), Timeout(Span.Max)) {
          case DataStoreResults.Errored(item, message) => message should be(Commons.DatastoreErrorMessages.DuplicateKeyError)
          case _ => fail("should have encountered a Failure in the datastore (Duplicate Key)")
        }

        assertLogEvents("insertConfiguration", 1, "added-configuration")
      }
    }

    describe("update") {

      it("Updates an existing configuration") {
        val modifiedConfiguration = testConfiguration.copy(
          temporality = Temporality(
            testConfiguration.temporality.effectiveAt,
            testConfiguration.temporality.expiresAt,
            5000))
        whenReady(settingsDataStore.updateConfiguration(modifiedConfiguration), Timeout(Span.Max)) {
          dsr =>
            dsr should be(DataStoreResults.Updated(testConfiguration, modifiedConfiguration))
        }

        assertLogEvents("updateConfiguration", 1, "original-configuration", "validated-configuration")
      }

      it("fails to update a configuration that does not exist") {
        val modifiedConfiguration = testConfiguration.copy(
          _id = UUID.randomUUID(),
          temporality = Temporality(
            testConfiguration.temporality.effectiveAt,
            testConfiguration.temporality.expiresAt,
            5000))
        whenReady(settingsDataStore.updateConfiguration(modifiedConfiguration), Timeout(Span.Max)) {
          dsr =>
            dsr should be(DataStoreResults.NotFound(Some(modifiedConfiguration), Commons.DatastoreErrorMessages.NotFoundError))
        }

        assertLogEvents("updateConfiguration", 1, "original-configuration", "validated-configuration")
      }
    }

    describe("retrieval") {

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
          case NotFound(Some(config), msg) => fail(s"NotFound -> $msg")
        }

        assertLogEvents("retrieveConfiguration", 1, prodEnvironment, application3, loggingScope, logLevelSetting)
      }

      it("Returns a NotFound when the environment / application / scope / setting combination does not exist") {
        whenReady(settingsDataStore.retrieveConfiguration(nonExistentSegment, nonExistentSegment, nonExistentSegment, nonExistentSegment), Timeout(Span.Max)) {
          result =>
            result should be(DataStoreResults.NotFound(None, s"environment '$nonExistentSegment' / application '$nonExistentSegment' / scope '$nonExistentSegment' / setting '$nonExistentSegment' combination was not found"))
        }

        assertLogEvents("retrieveConfiguration", 1, nonExistentSegment)
      }

      it("Returns a NotFound when there is no active configuration") {
        whenReady(settingsDataStore.retrieveConfiguration(devEnvironment, application, loggingScope, logLevelSetting), Timeout(Span.Max)) {
          result =>
            result should be(DataStoreResults.NotFound(None, s"environment '$devEnvironment' / application '$application' / scope '$loggingScope' / setting '$logLevelSetting' found no active configuration"))
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
          JsString("UNIQUE_VALUE"),
          DateTime.now().minusDays(2),
          DateTime.now().plusDays(2),
          10L
        )

        whenReady(settingsDataStore.insertConfiguration(configurationWithSourceId), Timeout(Span.Max)) {
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
          JsObject(Map("KEY" -> JsString("UNIQUE_VALUE"))),
          DateTime.now().minusDays(2),
          DateTime.now().plusDays(2),
          10L
        )

        val expectedSourceId = "1234567890123456789012345678901234567890123456789012345678901234"
        val expectedKey = ConfigurationFactory.Key(uniqueEnvironment, uniqueApplication, uniqueScope, uniqueSetting, Some(expectedSourceId))
        val expectedConfiguration = configurationWithSourceId.copy(key = expectedKey)

        whenReady(settingsDataStore.insertConfiguration(configurationWithSourceId), Timeout(Span.Max)) {
          retrievedConfiguration =>
            retrievedConfiguration should be(DataStoreResults.Added(expectedConfiguration))
        }

        assertLogEvents("insertConfiguration", 1, "given-configuration", "added-configuration", sourceId, expectedSourceId)
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
          case NotFound(_, msg) => fail(s"NotFound -> $msg")
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
          case NotFound(None, msg) => fail(s"NotFound -> $msg")
        }
      }

      it("does not use wildcards inappropriately") {
        whenReady(settingsDataStore.retrieveConfiguration(app, env, scp1, stg), Timeout(Span.Max)) {
          case Found(configurations) ⇒
            val configurationList = configurations.asInstanceOf[List[Configuration]]
            configurationList.size should be(1)
          case NotFound(None, msg) ⇒ fail(s"NotFound -> $msg")
        }
      }

      it("loads the configuration schedule in ascending effectiveAt order") {
        whenReady(settingsDataStore.retrieveConfigurationSchedule(app, env, scpSched, stg)) {
          case Found(configurations) =>
            val configurationList = configurations.asInstanceOf[List[Configuration]]
            configurationList.size should be(4)

            println(s"${configurationList.toJson.prettyPrint}")

            configurationList.apply(0) should be(scheduled1)
            configurationList.apply(1) should be(scheduled3)
            configurationList.apply(2) should be(scheduled2)
            configurationList.apply(3) should be(scheduled4)
        }
      }
    }

    describe("delete") {

    }

    describe("Bulk Operations") {

      val originalConfigurations = (for {
        i <- 1 to 10
      } yield {
          TestObjectGenerator.configuration()
        }).toList

      val modifiedConfigurations = originalConfigurations map { configuration => configuration.copy(value = TestObjectGenerator.randomJsString()) }
      val nonExistentConfigurations = originalConfigurations map { configuration => configuration.copy(_id = UUID.randomUUID(), value = JsString("NONEXISTENT")) }
      val mixedConfigurations = modifiedConfigurations zip nonExistentConfigurations flatMap { case (l, r) => Seq(l, r) }

      val originalConfigurationList = ConfigurationList(originalConfigurations)
      val modifiedConfigurationList = ConfigurationList(modifiedConfigurations)
      val nonExistentConfigurationList = ConfigurationList(nonExistentConfigurations)
      val mixedConfigurationList = ConfigurationList(mixedConfigurations)

      it("inserts a list of Configurations successfully") {
        whenReady(settingsDataStore.bulkInsertConfigurations(originalConfigurationList)) {
          dsrs =>
            for {
              index <- originalConfigurations.indices
              configuration = originalConfigurations.apply(index)
              dsr = dsrs.apply(index)
            } yield {
              dsr should be(DataStoreResults.Added(configuration))
            }
        }
      }

      it("fails to insert a list of Configurations that exist already") {
        whenReady(settingsDataStore.bulkInsertConfigurations(originalConfigurationList)) {
          dsrs =>
            for {
              index <- originalConfigurations.indices
              configuration = originalConfigurations.apply(index)
              dsr = dsrs.apply(index)
            } yield {
              dsr should be(DataStoreResults.Errored(configuration, Commons.DatastoreErrorMessages.DuplicateKeyError))
            }
        }
      }

      it("updates a list of Configurations successfully") {
        whenReady(settingsDataStore.bulkUpdateConfigurations(modifiedConfigurationList)) {
          dsrs =>
            for {
              index <- modifiedConfigurations.indices
              originalConfiguration = originalConfigurations.apply(index)
              updatedConfiguration = modifiedConfigurations.apply(index)
              dsr = dsrs.apply(index)
            } yield {
              dsr should be(DataStoreResults.Updated(originalConfiguration, updatedConfiguration))
            }
        }
      }

      it("fails to update a list of Configurations that exist already") {
        whenReady(settingsDataStore.bulkUpdateConfigurations(nonExistentConfigurationList)) {
          dsrs =>
            for {
              index <- nonExistentConfigurations.indices
              configuration = nonExistentConfigurations.apply(index)
              dsr = dsrs.apply(index)
            } yield {
              dsr should be(DataStoreResults.NotFound(Some(configuration), Commons.DatastoreErrorMessages.NotFoundError))
            }
        }
      }

      it("handles a mix of existing and non-existing Configuration on an update") {
        whenReady(settingsDataStore.bulkUpdateConfigurations(mixedConfigurationList)) {
          dsrs =>
            for {
              index <- mixedConfigurations.indices
              configuration = mixedConfigurations.apply(index)
              dsr = dsrs.apply(index)
            } yield {
              dsr match {
                case DataStoreResults.Updated(originalConfiguration, modifiedConfiguration) =>
                  modifiedConfiguration should be(configuration)
                case NotFound(Some(notFoundConfiguration), message) =>
                  notFoundConfiguration should be(configuration)
                  message should be(Commons.DatastoreErrorMessages.NotFoundError)
              }
            }
        }
      }
    }
  }

  describe("environments") {
    it("Returns the correct environments") {
      val expectedEnvironments = List[String](uniqueEnvironment, "default", devEnvironment, sourceIdEnvironment, wildcardEnvironment, prodEnvironment, qaEnvironment, testEnvironment)
        .sortBy(s => s)
      whenReady(settingsDataStore.retrieveEnvironments(), Timeout(Span.Max)) {
        case Found(environments) =>
          expectedEnvironments map { environment => environments should contain(environment) }
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
        result should be(DataStoreResults.NotFound(None, s"environment '$nonExistentSegment' was not found"))
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
        result should be(DataStoreResults.NotFound(None, s"environment '$nonExistentSegment' / application '$nonExistentSegment' combination was not found"))
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
        result should be(DataStoreResults.NotFound(None, s"environment '$nonExistentSegment' / application '$nonExistentSegment' / scope '$nonExistentSegment' combination was not found"))
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
