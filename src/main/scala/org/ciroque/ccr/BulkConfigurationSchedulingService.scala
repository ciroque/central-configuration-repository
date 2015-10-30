package org.ciroque.ccr

import scala.concurrent.ExecutionContext.Implicits.global
import org.ciroque.ccr.core.{Commons, CcrService}
import org.ciroque.ccr.datastores.DataStoreResults._
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
              recordValue("configurations", configurations.toJson.toString())
              val dsr = dataStore.bulkInsertConfigurations(configurations)
              val bulkConfigurationInsertResponse = dsr.map {
                dataStoreResults ⇒
                  val bulkConfigurationStatuses = dataStoreResults.map {
                    case Added(config: Configuration) ⇒ BulkConfigurationStatus(StatusCodes.Created.intValue, config, "")
                    case Errored(item: Configuration, msg: String) ⇒ BulkConfigurationStatus(StatusCodes.UnprocessableEntity.intValue, item, "", message = Some(msg))
                    case Failure(msg: String, cause: Throwable) ⇒ BulkConfigurationStatus(StatusCodes.InternalServerError.intValue, null, msg)
                  }

                  BulkConfigurationInsertResponse(bulkConfigurationStatuses)
              }
              for {
                eventualResult ← bulkConfigurationInsertResponse
              } yield {
                recordValue("result", eventualResult.toJson.toString())
                context.complete(HttpResponse(
                  if(eventualResult.isSuccess) StatusCodes.Created else StatusCodes.MultiStatus,
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
