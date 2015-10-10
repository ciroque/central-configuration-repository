package org.ciroque.ccr

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.ciroque.ccr.core.{CcrService, Commons}
import org.ciroque.ccr.datastores.DataStoreResults._
import org.ciroque.ccr.datastores.SettingsDataStore
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.models.ConfigurationFactory.Configuration
import org.ciroque.ccr.responses.ConfigurationResponse
import spray.http.MediaTypes._
import spray.http.{HttpEntity, HttpResponse, StatusCode, StatusCodes}
import spray.json._
import spray.routing.HttpService

import scala.concurrent.ExecutionContext.Implicits.global

trait ConfigurationManagementService
  extends HttpService
  with CcrService {

  implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)
  implicit val dataStore: SettingsDataStore

  override def getVersion = new SemanticVersion(1, 0, 0)

  def routes = settingUpsertRoute

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
                val (result: JsValue, statusCode: StatusCode) = processDataStoreResult(eventualResult)
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

  def processDataStoreResult(eventualResult: DataStoreResult): (JsValue, StatusCode) = {
    import org.ciroque.ccr.responses.ConfigurationResponseProtocol._
    val (result: JsValue, statusCode: StatusCode) = eventualResult match {
      case Added(item: Configuration) => (ConfigurationResponse(List(item)).toJson, StatusCodes.OK)
      case Failure(message, cause) => Commons.failureResponseFactory(message, cause)
    }
    (result, statusCode)
  }
}
