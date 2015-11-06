package org.ciroque.ccr

import java.util.UUID

import akka.actor.ActorRefFactory
import org.ciroque.ccr.core.Commons
import org.ciroque.ccr.datastores.{DataStoreResults, SettingsDataStore}
import org.ciroque.ccr.helpers.{TestHelpers, TestObjectGenerator}
import org.ciroque.ccr.logging.CachingLogger
import org.ciroque.ccr.models.ConfigurationFactory.AuditHistory
import org.ciroque.ccr.responses.AuditHistoryResponse
import org.joda.time.DateTime
import org.scalatest.mock.EasyMockSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import spray.http.StatusCodes
import spray.testkit.ScalatestRouteTest

import scala.concurrent.Future
import spray.httpx.SprayJsonSupport._

class ConfigurationAuditServiceTests
  extends FunSpec
  with ConfigurationAuditService
  with ScalatestRouteTest
  with Matchers
  with BeforeAndAfterEach
  with EasyMockSugar
  with TestHelpers {

  override def actorRefFactory: ActorRefFactory = system
  override implicit val dataStore: SettingsDataStore = mock[SettingsDataStore]
  override implicit val logger = new CachingLogger()

  describe("auditing endpoint") {
    it("requires an id and returns an appropriate list of auditing changes") {
      val uuid: UUID = UUID.randomUUID()
      val now = DateTime.now
      val firstDate = now.minusMonths(6)
      val secondDate = now.minusMonths(4)
      val thirdDate = now.minusMonths(2)
      val firstHistory = (TestObjectGenerator.configuration(uuid), None)
      val secondHistory = (TestObjectGenerator.configuration(uuid), Some(TestObjectGenerator.configuration(uuid)))
      val thirdHistory = (TestObjectGenerator.configuration(uuid), Some(TestObjectGenerator.configuration(uuid)))
      val auditHistory = List(
        AuditHistory(firstDate, firstHistory._1, firstHistory._2),
        AuditHistory(secondDate, secondHistory._1, secondHistory._2),
        AuditHistory(thirdDate, thirdHistory._1, thirdHistory._2)
      )
      val dataStoreResults = DataStoreResults.Found(auditHistory)

      expecting {
        dataStore.retrieveAuditHistory(uuid).andReturn(Future.successful(dataStoreResults))
      }
      whenExecuting(dataStore) {
        Get(s"/${Commons.rootPath}/${Commons.serviceSegment}/${Commons.auditSegment}/${uuid.toString}") ~> routes ~> check {
          import org.ciroque.ccr.responses.AuditHistoryResponseProtocol._
          status should be(StatusCodes.OK)
          assertCorsHeaders(headers)
          val response = responseAs[AuditHistoryResponse]
          response.history.length should be(3)
          for {
            index <- auditHistory.indices
          } yield {
            response.history.apply(index).toJson should be(response.history.apply(index).toJson)
          }
        }
      }
    }
  }
}
