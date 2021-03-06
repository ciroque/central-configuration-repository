package org.ciroque.ccr

import akka.actor.ActorRefFactory
import org.ciroque.ccr.core.Commons
import org.ciroque.ccr.datastores.{DataStoreResults, SettingsDataStore}
import org.ciroque.ccr.helpers.TestObjectGenerator
import org.ciroque.ccr.logging.CachingLogger
import org.ciroque.ccr.models.ConfigurationFactory._
import org.ciroque.ccr.responses._
import org.ciroque.ccr.responses.BulkConfigurationResponseProtocol._
import org.easymock.EasyMock._
import org.scalatest._
import org.scalatest.mock.EasyMockSugar
import org.slf4j.Logger
import spray.http.StatusCodes
import spray.testkit.ScalatestRouteTest
import spray.httpx.SprayJsonSupport._
import spray.json._

import scala.concurrent.Future

class BulkConfigurationSchedulingServiceTests extends FunSpec
  with BulkConfigurationSchedulingService
  with ScalatestRouteTest
  with Matchers
  with BeforeAndAfterEach
  with EasyMockSugar {

  override def actorRefFactory: ActorRefFactory = system

  override implicit val dataStore: SettingsDataStore = mock[SettingsDataStore]
  implicit val logger: Logger = new CachingLogger()

  override def beforeEach() = reset(dataStore)

  describe("BulkConfigurationSchedulingServiceTests") {

    describe("bulk insert") {

      val configurationList = ConfigurationList(List(
        TestObjectGenerator.configuration(),
        TestObjectGenerator.configuration(),
        TestObjectGenerator.configuration()
      ))

      it("should accept a Sequence of Configurations via POST and insert them into the datastore") {
        val dataStoreResults = Future.successful(configurationList.configurations.map {
          config ⇒ DataStoreResults.Added(config)
        })

        val expectedBulkConfigurationInsertResponse = BulkConfigurationResponse(configurationList.configurations.map {
          config ⇒ BulkConfigurationStatusFactory(201, config)
        })

        expecting {
          dataStore.bulkInsertConfigurations(isA(classOf[ConfigurationList])).andReturn(dataStoreResults)
        }
        whenExecuting(dataStore) {
          Post(s"/${Commons.rootPath}/${Commons.schedulingSegment}/${Commons.bulkSegment}", configurationList) ~> routes ~> check {
            status should equal(StatusCodes.Created)
            val actualBulkConfigurationInsertResponse = responseAs[BulkConfigurationResponse]
            actualBulkConfigurationInsertResponse.toJson should be(expectedBulkConfigurationInsertResponse.toJson)
          }
        }
      }

      it("should return a 207 MultiStatus and appropriate individual results when one insert fails") {
        val ErrorMessage = "Just Dumb Luck"
        val configurations = configurationList.configurations
        val dataStoreResults = Future.successful(List(
          DataStoreResults.Added(configurations.head),
          DataStoreResults.Errored(configurations.apply(1), ErrorMessage),
          DataStoreResults.Added(configurations.apply(2))
        ))

        val expectedBulkConfigurationInsertResponse = BulkConfigurationResponse(
          List(
            BulkConfigurationStatusFactory(201, configurations.head),
            BulkConfigurationStatusFactory(422, configurations.apply(1), ErrorMessage),
            BulkConfigurationStatusFactory(201, configurations.apply(2))
          ))

        expecting {
          dataStore.bulkInsertConfigurations(isA(classOf[ConfigurationList])).andReturn(dataStoreResults)
        }
        whenExecuting(dataStore) {
          Post(s"/${Commons.rootPath}/${Commons.schedulingSegment}/${Commons.bulkSegment}", configurationList) ~> routes ~> check {
            status should equal(StatusCodes.MultiStatus)
            val actualBulkConfigurationInsertResponse = responseAs[BulkConfigurationResponse]
            actualBulkConfigurationInsertResponse.toJson should be(expectedBulkConfigurationInsertResponse.toJson)
          }
        }
      }

      it("handles an exception in dataStore::bulkInsertConfigurations") {
        val ErrorMessage = "Just Dumb Luck"
        val configurations = configurationList.configurations

        expecting {
          dataStore.bulkInsertConfigurations(isA(classOf[ConfigurationList])).andThrow(new IllegalArgumentException(ErrorMessage))
        }
        whenExecuting(dataStore) {
          Post(s"/${Commons.rootPath}/${Commons.schedulingSegment}/${Commons.bulkSegment}", configurationList) ~> routes ~> check {
            status should equal(StatusCodes.InternalServerError)
          }
        }
      }
    }

    describe("bulk update") {
      val cfg1 = TestObjectGenerator.configuration()
      val cfg2 = TestObjectGenerator.configuration()
      val cfg3 = TestObjectGenerator.configuration()

      val configurations = List(cfg1, cfg2, cfg3)

      val updatedConfigurationList = configurations.map(_.copy(value = TestObjectGenerator.randomJsString()))

      it("should accept a Sequence of Configurations via PUT and update them in the datastore") {
        val dataStoreResults = for {
          index ← configurations.indices
        } yield {
          val original = configurations.apply(index)
          val updated = updatedConfigurationList.apply(index)
          DataStoreResults.Updated(original, updated)
        }

        val bulkConfigurationStatuses = for {
          index ← configurations.indices
        } yield {
          val original = configurations.apply(index)
          val updated = updatedConfigurationList.apply(index)
          BulkConfigurationStatusFactory(200, original, updated)
        }

        val expectedBulkConfigurationResponse = BulkConfigurationResponse(bulkConfigurationStatuses.toList)

        expecting {
          dataStore.bulkUpdateConfigurations(isA(classOf[ConfigurationList])).andReturn(Future.successful(dataStoreResults.toList))
        }
        whenExecuting(dataStore) {
          Put(s"/${Commons.rootPath}/${Commons.schedulingSegment}/${Commons.bulkSegment}", ConfigurationList(configurations)) ~> routes ~> check {
            status should equal(StatusCodes.OK)
            val actualBulkConfigurationResponse = responseAs[BulkConfigurationResponse]
            actualBulkConfigurationResponse.toJson should be(expectedBulkConfigurationResponse.toJson)
          }
        }
      }

      it("should return a 207 MultiStatus and appropriate individual results when one update fails") {
        val FailIndex = 1
        val ErrorMessage = "Something Stupid"
        val dataStoreResults = for {
          index ← configurations.indices
        } yield {
          val original = configurations.apply(index)
          val updated = updatedConfigurationList.apply(index)
          if(index == FailIndex)
            DataStoreResults.Errored(original, ErrorMessage)
          else
            DataStoreResults.Updated(original, updated)
        }

        val bulkConfigurationStatuses = for {
          index ← configurations.indices
        } yield {
          val original = configurations.apply(index)
          if(index == FailIndex)
            BulkConfigurationStatusFactory(422, original, ErrorMessage)
          else {
            val updated = updatedConfigurationList.apply(index)
            BulkConfigurationStatusFactory(200, original, updated)
          }
        }

        val expectedBulkConfigurationResponse = BulkConfigurationResponse(bulkConfigurationStatuses.toList)

        expecting {
          dataStore.bulkUpdateConfigurations(isA(classOf[ConfigurationList])).andReturn(Future.successful(dataStoreResults.toList))
        }
        whenExecuting(dataStore) {
          Put(s"/${Commons.rootPath}/${Commons.schedulingSegment}/${Commons.bulkSegment}", ConfigurationList(configurations)) ~> routes ~> check {
            status should equal(StatusCodes.MultiStatus)
            val actualBulkConfigurationResponse = responseAs[BulkConfigurationResponse]
            actualBulkConfigurationResponse.toJson should be(expectedBulkConfigurationResponse.toJson)
          }
        }
      }

      it("handles an exception in dataStore::bulkUpdateConfigurations") {
        val ErrorMessage = "Just Dumb Luck"
        expecting {
          dataStore.bulkUpdateConfigurations(isA(classOf[ConfigurationList])).andThrow(new IllegalArgumentException(ErrorMessage))
        }
        whenExecuting(dataStore) {
          Put(s"/${Commons.rootPath}/${Commons.schedulingSegment}/${Commons.bulkSegment}", ConfigurationList(configurations)) ~> routes ~> check {
            status should equal(StatusCodes.InternalServerError)
          }
        }
      }
    }
  }
}
