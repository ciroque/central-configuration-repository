package org.ciroque.ccr

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.ciroque.ccr.core.DataStoreResults._
import org.ciroque.ccr.core.{Commons, SettingsDataStore}
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.models.ConfigurationFactory.Configuration
import org.ciroque.ccr.responses.ConfigurationResponse
import spray.routing.HttpService
import scala.concurrent.ExecutionContext.Implicits.global
import spray.json._

trait ConfigurationManagementService
  extends HttpService {

  implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)
  implicit val dataStore: SettingsDataStore

  def settingUpsertRoute = pathPrefix(Commons.rootPath / Commons.managementSegment / Segment / Segment / Segment / Segment) {
    (environment, application, scope, setting) =>
      println(s"ConfigurationManagerService::settingUpsertRoute($environment, $application, $scope, $setting)")
      pathEndOrSingleSlash {
        requestUri { uri =>
          import spray.httpx.SprayJsonSupport._
          entity(as[ConfigurationFactory.Configuration]) { configuration =>
            post {
              respondWithHeaders(Commons.corsHeaders) {
                val eventualValue = for {
                  result <- dataStore.upsertConfiguration(configuration)
                } yield {
                    import org.ciroque.ccr.responses.ConfigurationResponseProtocol._
                    result match {
                      case Added(item: Configuration) => ConfigurationResponse(List(item)).toJson
                      case _ => JsString("")
                    }
                  }
                complete(eventualValue.toString)
              }
            }
          }
        }
      }
  }

  def routes = settingUpsertRoute
}
