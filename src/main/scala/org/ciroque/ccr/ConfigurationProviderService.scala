package org.ciroque.ccr

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.ciroque.ccr.core.{SettingsDataStore, Commons}
import org.ciroque.ccr.responses.{EnvironmentResponse, RootResponseProtocol, RootResponse}
import spray.http.MediaTypes._
import spray.routing.{PathMatchers, HttpService}
import spray.httpx.SprayJsonSupport.sprayJsonMarshaller

trait ConfigurationProviderService extends HttpService {
  implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)
  implicit val dataStore: SettingsDataStore

  def rootRoute = pathEndOrSingleSlash {
    get {
      respondWithMediaType(`application/json`) {
        respondWithHeaders(Commons.corsHeaders) {
          complete {
            import RootResponseProtocol.RootResponseFormat
            RootResponse("Please review the documentation to learn how to use this service.", Map("documentation" -> "/documentation"))
          }
        }
      }
    }
  }

  def environmentsRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment) {
    pathEndOrSingleSlash {
      get {
        respondWithMediaType(`application/json`) {
          respondWithHeaders(Commons.corsHeaders) {
            complete {
              new EnvironmentResponse(dataStore.retrieveEnvironments.getOrElse(List()))
            }
          }
        }
      }
    }
  }

  def applicationsRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / PathMatchers.Segment) {
    path =>
    pathEndOrSingleSlash {
      get {
        respondWithMediaType(`application/json`) {
          respondWithHeaders(Commons.corsHeaders) {
            complete {
              new EnvironmentResponse(dataStore.retrieveApplications(path).getOrElse(List()))
            }
          }
        }
      }
    }
  }

  def routes = rootRoute ~ environmentsRoute ~ applicationsRoute
}
