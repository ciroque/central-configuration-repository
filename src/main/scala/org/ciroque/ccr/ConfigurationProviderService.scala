package org.ciroque.ccr

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.ciroque.ccr.core.{Commons, SettingsDataStore}
import org.ciroque.ccr.responses._
import spray.http.MediaTypes._
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport.sprayJsonMarshaller
import spray.routing.HttpService

trait ConfigurationProviderService extends HttpService {
  implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)
  implicit val dataStore: SettingsDataStore

  def defaultRoute = pathEndOrSingleSlash {
    get {
      respondWithMediaType(`application/json`) {
        respondWithHeaders(Commons.corsHeaders) {
          complete {
            import org.ciroque.ccr.responses.RootResponseProtocol.RootResponseFormat
            RootResponse("Please review the documentation to learn how to use this service.", Map("documentation" -> "/documentation"))
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

  def environmentRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment) {
    environment =>
      pathEndOrSingleSlash {
        get {
          val applications = dataStore.retrieveApplications(environment)
          respondWithMediaType(`application/json`) {
            respondWithHeaders(Commons.corsHeaders) {
              applications match {
                case Some(apps) => complete {
                  new InterstitialResponse(apps)
                }
                case None => respondWithStatus(StatusCodes.NotFound) {
                  complete {
                    ""
                  } // TODO: This should be something appropriate.
                }
              }
            }
          }
        }
      }
  }

  def applicationsRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment) {
    (environment, application) =>
      pathEndOrSingleSlash {
        get {
          val loadedScopes = dataStore.retrieveScopes(environment, application)
          respondWithMediaType(`application/json`) {
            respondWithHeaders(Commons.corsHeaders) {
              loadedScopes match {
                case Some(scopes) => complete {
                  new InterstitialResponse(scopes)
                }
                case None =>
                  respondWithStatus(StatusCodes.NotFound) {
                    complete {
                      ""
                    } // TODO: This should be something appropriate.
                  }
              }
            }
          }
        }
      }
  }

  def scopeRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment / Segment) {
    (environment, application, scope) =>
      pathEndOrSingleSlash {
        get {
          val loadedSettingNames = dataStore.retrieveSettingNames(environment, application, scope)
          respondWithMediaType(`application/json`) {
            respondWithHeaders(Commons.corsHeaders) {
              loadedSettingNames match {
                case Some(settingNames) => complete {
                  new InterstitialResponse(settingNames)
                }
                case None =>
                  respondWithStatus(StatusCodes.NotFound) {
                    complete {
                      ""
                    } // TODO: This should be something appropriate.
                  }
              }
            }
          }
        }
      }
  }

  def settingRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment / Segment / Segment) {
    (environment, application, scope, setting) =>
      pathEndOrSingleSlash {
        get {
          val result = dataStore.retrieveSetting(environment, application, scope, setting)
          respondWithMediaType(`application/json`) {
            respondWithHeaders(Commons.corsHeaders) {
              result match {
                case Some(_) => complete {
                  new SettingResponse(result)
                }
                case None =>
                  respondWithStatus(StatusCodes.NotFound) {
                    complete {
                      ""
                    } // TODO: This should be something appropriate.
                  }
              }
            }
          }
        }
      }
  }

  def routes = defaultRoute ~ environmentRoute ~ rootRoute ~ applicationsRoute ~ settingRoute ~ scopeRoute
}
