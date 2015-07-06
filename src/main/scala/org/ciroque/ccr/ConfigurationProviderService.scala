package org.ciroque.ccr

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.ciroque.ccr.core.{CcrTypes, Commons, SettingsDataStore}
import org.ciroque.ccr.responses._
import spray.http.MediaTypes._
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport.sprayJsonMarshaller
import spray.routing.HttpService

trait ConfigurationProviderService
  extends HttpService
  with CcrTypes {

  implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)
  implicit val dataStore: SettingsDataStore

  private def completeInterstitialRoute(result: Option[List[String]], notFoundMessage: String) = {
    respondWithMediaType(`application/json`) {
      respondWithHeaders(Commons.corsHeaders) {
        result match {
          case Some(list) => complete {
            new InterstitialGetResponse(list)
          }
          case None => respondWithStatus(StatusCodes.NotFound) {
            complete {
              notFoundMessage
            }
          }
        }
      }
    }
  }

  def defaultRoute = pathEndOrSingleSlash {
    get {
      respondWithMediaType(`application/json`) {
        respondWithHeaders(Commons.corsHeaders) {
          respondWithStatus(Commons.teaPotStatusCode) {
            complete {
              import org.ciroque.ccr.responses.RootResponseProtocol.RootResponseFormat
              RootResponse("Please review the documentation to learn how to use this service.", Map("documentation" -> "/documentation"))
            }
          }
        }
      }
    }
  }

  def rootRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment) {
    pathEndOrSingleSlash {
      get {
        completeInterstitialRoute(dataStore.retrieveEnvironments, s"")
      }
    }
  }

  def environmentRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment) {
    environment =>
      pathEndOrSingleSlash {
        get {
          completeInterstitialRoute(
            dataStore.retrieveApplications(environment),
            s"environment '$environment' was not found")
        }
      }
  }

  def applicationsRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment) {
    (environment, application) =>
      pathEndOrSingleSlash {
        get {
          completeInterstitialRoute(
            dataStore.retrieveScopes(environment, application),
            s"application '$application' in environment '$environment' was not found"
          )
        }
      }
  }

  def scopeRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment / Segment) {
    (environment, application, scope) =>
      pathEndOrSingleSlash {
        get {
          completeInterstitialRoute(
            dataStore.retrieveSettingNames(environment, application, scope),
            s"scope '$scope' for application '$application' in environment '$environment' was not found"
          )
        }
      }
  }

  def settingRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment / Segment / Segment) {
    (environment, application, scope, setting) =>
      pathEndOrSingleSlash {
        get {
          respondWithMediaType(`application/json`) {
            respondWithHeaders(Commons.corsHeaders) {
              dataStore.retrieveSetting(environment, application, scope, setting) match {
                case Some(_) => complete {
                  new SettingResponse(dataStore.retrieveSetting(environment, application, scope, setting))
                }
                case None =>
                  respondWithStatus(StatusCodes.NotFound) {
                    complete {
                      s"setting '$setting' in scope '$scope' for application '$application' in environment '$environment' was not found"
                    }
                  }
              }
            }
          }
        }
      }
  }

  def routes = defaultRoute ~ environmentRoute ~ rootRoute ~ applicationsRoute ~ settingRoute ~ scopeRoute
}
