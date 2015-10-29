package org.ciroque.ccr

import akka.actor.ActorRefFactory
import org.scalatest._
import org.scalatest.mock.EasyMockSugar
import spray.testkit.ScalatestRouteTest

class ConfigurationBatchSchedulingServiceTests extends FunSpec
  with ConfigurationBatchSchedulingService
  with ScalatestRouteTest
  with Matchers
  with BeforeAndAfterEach
  with EasyMockSugar {

  override def getVersion: SemanticVersion = SemanticVersion(1, 0, 1)
  override implicit def actorRefFactory: ActorRefFactory = system

  describe("ConfigurationBatchSchedulingService") {
    describe("bulk insert") {

    }

    describe("bulk update") {

    }
  }
}