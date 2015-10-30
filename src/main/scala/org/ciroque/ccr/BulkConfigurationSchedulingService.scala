package org.ciroque.ccr

import scala.concurrent.ExecutionContext.Implicits.global
import org.ciroque.ccr.core.{Commons, CcrService}
import org.ciroque.ccr.datastores.DataStoreResults.Added
import org.ciroque.ccr.datastores.SettingsDataStore
import org.ciroque.ccr.logging.ImplicitLogging._
import org.ciroque.ccr.models.ConfigurationFactory._
import org.ciroque.ccr.responses.{BulkConfigurationInsertResponse, BulkConfigurationStatus}
import org.ciroque.ccr.responses.BulkConfigurationResponseProtocol._
import org.slf4j.Logger
import spray.http.MediaTypes._
import spray.http.{HttpEntity, StatusCodes, HttpResponse}
import spray.routing.HttpService
import spray.httpx.SprayJsonSupport._
import spray.json._

trait BulkConfigurationSchedulingService
  extends HttpService
  with CcrService {

  implicit val dataStore: SettingsDataStore
  implicit val logger: Logger

  override def getVersion = new SemanticVersion(1, 0, 0)

  def rootBulkRoute = path(Commons.rootPath / Commons.schedulingSegment / Commons.bulkSegment) {
    pathEndOrSingleSlash {
      entity(as[ConfigurationList]) {
        configurations ⇒
          post { context ⇒
            withImplicitLogging("ConfigurationBulkSchedulingService::POST") {
              val dsr = dataStore.bulkInsertConfigurations(configurations)
              val bulkConfigurationInsertResponse = dsr.map {
                dataStoreResults ⇒
                  val bulkConfigurationStatuses = dataStoreResults.map {
                    case Added(config: Configuration) ⇒ BulkConfigurationStatus(StatusCodes.Created.intValue, config, "")
                    case _ ⇒ BulkConfigurationStatus(StatusCodes.InternalServerError.intValue, null, "")
                  }

                  BulkConfigurationInsertResponse(bulkConfigurationStatuses)
              }
              for {
                eventualResult ← bulkConfigurationInsertResponse
              } yield {

                context.complete(HttpResponse(
                  StatusCodes.Created,
                  HttpEntity(`application/json`, eventualResult.toJson.toString()),
                  Commons.corsHeaders))
              }
            }
          }
      }
    }
  }

  def routes = rootBulkRoute
}
