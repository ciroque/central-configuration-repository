package org.ciroque.ccr

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.ciroque.ccr.core.{SettingsDataStore, Commons}
import org.ciroque.ccr.responses._
import spray.http.MediaTypes._
import spray.routing.{PathMatchers, HttpService}
import spray.httpx.SprayJsonSupport.sprayJsonMarshaller

trait ConfigurationProviderService extends HttpService {
  implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)
  implicit val dataStore: SettingsDataStore

  def defaultRoute = pathEndOrSingleSlash {
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

  def environmentRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / PathMatchers.Segment) {
    environment =>
    pathEndOrSingleSlash {
      get {
        respondWithMediaType(`application/json`) {
          respondWithHeaders(Commons.corsHeaders) {
            complete {
              new InterstitialResponse(dataStore.retrieveApplications(environment).getOrElse(List()))
            }
          }
        }
      }
    }
  }

  def rootRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment) {
    pathEndOrSingleSlash {
      get {
        respondWithMediaType(`application/json`) {
          respondWithHeaders(Commons.corsHeaders) {
            complete {
              new InterstitialResponse(dataStore.retrieveEnvironments.getOrElse(List()))
            }
          }
        }
      }
    }
  }

  def scopesRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment) {
    (environment, application) =>
    pathEndOrSingleSlash {
      get {
        respondWithMediaType(`application/json`) {
          respondWithHeaders(Commons.corsHeaders) {
            complete {
              new InterstitialResponse(dataStore.retrieveScopes(environment, application).getOrElse(List()))
            }
          }
        }
      }
    }
  }

  def settingsRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment / Segment) {
    (environment, application, scope) =>
    pathEndOrSingleSlash {
      get {
        respondWithMediaType(`application/json`) {
          respondWithHeaders(Commons.corsHeaders) {
            complete {
              new InterstitialResponse(dataStore.retrieveSettingNames(environment, application, scope).getOrElse(List()))
            }
          }
        }
      }
    }
  }

  def routes = defaultRoute ~ environmentRoute ~ rootRoute ~ scopesRoute ~ settingsRoute
}
