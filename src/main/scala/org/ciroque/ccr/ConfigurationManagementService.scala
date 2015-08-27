package org.ciroque.ccr

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.ciroque.ccr.datastores.{SettingsDataStore, DataStoreResults}
import DataStoreResults._
import org.ciroque.ccr.core.Commons
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.models.ConfigurationFactory.Configuration
import org.ciroque.ccr.responses.ConfigurationResponse
import spray.http.MediaTypes._
import spray.http.{StatusCode, HttpResponse, HttpEntity, StatusCodes}
import spray.routing.HttpService
import scala.concurrent.ExecutionContext.Implicits.global
import spray.json._

trait ConfigurationManagementService
  extends HttpService {

  implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)
  implicit val dataStore: SettingsDataStore

  def settingUpsertRoute = pathPrefix(Commons.rootPath / Commons.managementSegment / Segment / Segment / Segment / Segment) {
    (environment, application, scope, setting) =>
      pathEndOrSingleSlash {
        requestUri { uri =>
          import spray.httpx.SprayJsonSupport._
          entity(as[ConfigurationFactory.Configuration]) { configuration =>
            post { context =>
              for {
                eventualResult <- dataStore.upsertConfiguration(configuration)
              } yield {
                import org.ciroque.ccr.responses.ConfigurationResponseProtocol._
                val (result: JsValue, statusCode: StatusCode) = eventualResult match {
                  case Added(item: Configuration) => (ConfigurationResponse(List(item)).toJson, StatusCodes.OK)
                  case Failure(message, cause) => Commons.failureResponseFactory(message, cause)
                }
                context.complete(HttpResponse(
                  statusCode,
                  HttpEntity(`application/json`, result.toString()),
                  Commons.corsHeaders))
              }
            }
          }
        }
      }
  }

  def routes = settingUpsertRoute
}
