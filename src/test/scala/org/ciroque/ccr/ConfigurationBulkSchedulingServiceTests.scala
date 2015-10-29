package org.ciroque.ccr

import akka.actor.ActorRefFactory
import org.ciroque.ccr.core.Commons
import org.ciroque.ccr.datastores.SettingsDataStore
import org.scalatest._
import org.scalatest.mock.EasyMockSugar
import spray.testkit.ScalatestRouteTest
import spray.routing._

class ConfigurationBulkSchedulingServiceTests extends FunSpec
  with ConfigurationBulkSchedulingService
  with ScalatestRouteTest
  with Matchers
  with BeforeAndAfterEach
  with EasyMockSugar {

  override def getVersion: SemanticVersion = SemanticVersion(1, 0, 1)
  override def actorRefFactory: ActorRefFactory = system
  override implicit val dataStore: SettingsDataStore = mock[SettingsDataStore]

  describe("ConfigurationBatchSchedulingService") {
    describe("bulk insert") {
      it("should accept a Sequence of Configurations via POST and insert them into the datastore") {
        Post(s"${Commons.rootPath}/${Commons.schedulingSegment}/${Commons.bulkSegment}") ~> routes ~> check {

        }
      }
    }

    describe("bulk update") {

    }
  }
}
