package org.ciroque.ccr

import java.util.UUID

import akka.actor.ActorRefFactory
import org.ciroque.ccr.core.Commons
import org.ciroque.ccr.datastores.{DataStoreResults, SettingsDataStore}
import org.ciroque.ccr.helpers.{TestHelpers, TestObjectGenerator}
import org.ciroque.ccr.logging.CachingLogger
import org.ciroque.ccr.models.ConfigurationFactory.{AuditHistory, AuditEntry}
import org.ciroque.ccr.responses.AuditHistoryResponse
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
      val auditEntries: List[AuditEntry] = TestObjectGenerator.auditHistoryList(uuid)
      val auditHistory: AuditHistory = AuditHistory(uuid, auditEntries)
      val dataStoreResults = DataStoreResults.Found(List(auditHistory))

      expecting {
        dataStore.retrieveAuditHistory(uuid).andReturn(Future.successful(dataStoreResults))
      }
      whenExecuting(dataStore) {
        Get(s"/${Commons.rootPath}/${Commons.auditSegment}/${Commons.serviceSegment}/${uuid.toString}") ~> routes ~> check {
          import org.ciroque.ccr.responses.AuditHistoryResponseProtocol._
          status should be(StatusCodes.OK)
          assertCorsHeaders(headers)
          val response = responseAs[AuditHistoryResponse]
          response.history.length should be(1)
          val histories = response.history.head

          for {
            index <- histories.events.indices
          } yield {
            histories.events.apply(index).toJson should be(auditEntries.apply(index).toJson)
          }
        }
      }
    }
  }
}
