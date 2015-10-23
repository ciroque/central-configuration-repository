package org.ciroque.ccr

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.ciroque.ccr.core.{CcrService, Commons}
import org.ciroque.ccr.datastores.DataStoreResults._
import org.ciroque.ccr.datastores.SettingsDataStore
import org.ciroque.ccr.logging.ImplicitLogging._
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.models.ConfigurationFactory.Configuration
import org.ciroque.ccr.responses.ConfigurationResponse
import org.slf4j.Logger
import spray.http.MediaTypes._
import spray.http.{HttpEntity, HttpResponse, StatusCode, StatusCodes}
import spray.json._
import spray.routing.HttpService

import scala.concurrent.ExecutionContext.Implicits.global

trait ConfigurationSchedulingService
  extends HttpService
  with CcrService {

  implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)
  implicit val dataStore: SettingsDataStore
  implicit val logger: Logger

  override def getVersion = new SemanticVersion(1, 0, 0)

  def routes = settingUpsertRoute

  def settingUpsertRoute = pathPrefix(Commons.rootPath / Commons.schedulingSegment) {
    pathEndOrSingleSlash {
      requestUri { uri =>
        import spray.httpx.SprayJsonSupport._
        entity(as[ConfigurationFactory.Configuration]) { configuration =>
          post { context =>
            withImplicitLogging("ConfigurationSchedulingService::settingUpsertRoute::POST") {
              for {
                eventualResult <- dataStore.upsertConfiguration(configuration)
              } yield {
                val (result: JsValue, statusCode: StatusCode) = processDataStoreResult(eventualResult)
                context.complete(HttpResponse(
                  statusCode,
                  HttpEntity(`application/json`, result.toString()),
                  Commons.corsHeaders))
              }
            }
          }
        } ~
        options { context ⇒
          withImplicitLogging("ConfigurationSchedulingService::settingUpsertRoute::OPTIONS") {
            context.complete(HttpResponse(StatusCodes.OK, HttpEntity(`application/json`, "[]"), Commons.corsHeaders))
          }
        }
      }
    }
  }

  def processDataStoreResult(eventualResult: DataStoreResult): (JsValue, StatusCode) = {
    import org.ciroque.ccr.responses.ConfigurationResponseProtocol._
    val (result: JsValue, statusCode: StatusCode) = eventualResult match {
      case Added(item: Configuration) => (ConfigurationResponse(List(item)).toJson, StatusCodes.OK)
      case Failure(message, cause) => Commons.failureResponseFactory(message, cause)
    }
    (result, statusCode)
  }
}