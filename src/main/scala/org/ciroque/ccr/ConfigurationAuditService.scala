package org.ciroque.ccr

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.ciroque.ccr.core.{Commons, CcrService}
import org.ciroque.ccr.datastores.DataStoreResults.{NotFound, Found}
import org.ciroque.ccr.datastores.SettingsDataStore
import org.ciroque.ccr.logging.ImplicitLogging._
import org.ciroque.ccr.models.ConfigurationFactory.AuditHistory
import org.ciroque.ccr.responses.AuditHistoryResponse
import org.slf4j.Logger
import spray.http.MediaTypes._
import spray.http.{HttpEntity, StatusCodes, HttpResponse}
import spray.routing.HttpService
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global

trait ConfigurationAuditService
  extends HttpService
  with CcrService {

  implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)

  override def getVersion = new SemanticVersion(1, 0, 0)

  implicit val dataStore: SettingsDataStore
  implicit val logger: Logger

  def auditRecordRoute = path(Commons.rootPath / Commons.serviceSegment / Commons.auditSegment / Segment) {
    uuid =>
      pathEndOrSingleSlash {
        get { context =>
          withImplicitLogging("ConfigurationAuditService::auditRecordRoute") {

            for {
              eventualResult <- dataStore.retrieveAuditHistory(UUID.fromString(uuid))
            } yield {
              import org.ciroque.ccr.responses.AuditHistoryResponseProtocol._
              val (statusCode, response) = eventualResult match {
                case Found(history: List[AuditHistory]) => (StatusCodes.OK, AuditHistoryResponse(history).toJson)
                case NotFound(Some(_), msg) => (StatusCodes.NotFound, JsString(msg))
              }

              context.complete(
                HttpResponse(
                  statusCode,
                  HttpEntity(`application/json`, response.toString),
                  Commons.corsHeaders
                )
              )
            }
          }
        }
      }
  }

  def routes = auditRecordRoute
}
