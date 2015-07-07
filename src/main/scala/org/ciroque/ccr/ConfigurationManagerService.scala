package org.ciroque.ccr

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.ciroque.ccr.core.DataStoreResults._
import org.ciroque.ccr.core.{Commons, SettingsDataStore}
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.responses.InterstitialPutResponse
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport.sprayJsonMarshaller
import spray.routing.HttpService

trait ConfigurationManagerService
  extends HttpService {

  implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)
  implicit val dataStore: SettingsDataStore

  def environmentCreationRoute = pathPrefix(Commons.rootPath / Commons.managementSegment / Segment) {
    (environment) =>
      pathEndOrSingleSlash {
        requestUri { uri =>
          put {
            respondWithHeaders(Commons.corsHeaders) {
              dataStore.createEnvironment(environment) match {
                case Success() => respondWithStatus(StatusCodes.Created) {
                  complete {
                    import org.ciroque.ccr.responses.InterstitialPutResponse._
                    new InterstitialPutResponse(uri.toString())
                  }
                }
                case Failure(_, _) => respondWithStatus(StatusCodes.UnprocessableEntity) {
                  complete {
                    ""
                  }
                }
              }
            }
          }
        }
      }
  }

  def applicationCreationRoute = pathPrefix(Commons.rootPath / Commons.managementSegment / Segment / Segment) {
    (environment, application) =>
      pathEndOrSingleSlash {
        requestUri { uri =>
          put {
            respondWithHeaders(Commons.corsHeaders) {
              dataStore.createApplication(environment, application) match {
                case Success() => respondWithStatus(StatusCodes.Created) {
                  complete {
                    import org.ciroque.ccr.responses.InterstitialPutResponse._
                    new InterstitialPutResponse(uri.toString())
                  }
                }
                case Failure(msg, _) => respondWithStatus(StatusCodes.UnprocessableEntity) {
                  complete {
                    msg
                  }
                }
              }
            }
          }
        }
      }
  }

  def scopeCreationRoute = pathPrefix(Commons.rootPath / Commons.managementSegment / Segment / Segment / Segment) {
    (environment, application, scope) =>
      pathEndOrSingleSlash {
        requestUri { uri =>
          put {
            respondWithHeaders(Commons.corsHeaders) {
              dataStore.createScope(environment, application, scope) match {
                case Success() => respondWithStatus(StatusCodes.Created) {
                  complete {
                    import org.ciroque.ccr.responses.InterstitialPutResponse._
                    new InterstitialPutResponse(uri.toString())
                  }
                }
                case Failure(msg, _) => respondWithStatus(StatusCodes.UnprocessableEntity) {
                  complete {
                    msg
                  }
                }
              }
            }
          }
        }
      }
  }

  def settingUpsertRoute = pathPrefix(Commons.rootPath / Commons.managementSegment / Segment / Segment / Segment / Segment) {
    (environment, application, scope, setting) =>
      pathEndOrSingleSlash {
        requestUri { uri =>
          import spray.httpx.SprayJsonSupport._
          entity(as[ConfigurationFactory.Configuration]) { configuration =>
            put {
              respondWithHeaders(Commons.corsHeaders) {
                dataStore.upsertSetting(environment, application, scope, setting, configuration) match {
                  case Success() => respondWithStatus(StatusCodes.Created) {
                    complete {
                      import org.ciroque.ccr.responses.InterstitialPutResponse._
                      new InterstitialPutResponse(uri.toString())
                    }
                  }
                  case Failure(msg, _) => respondWithStatus(StatusCodes.UnprocessableEntity) {
                    complete {
                      msg
                    }
                  }
                }
              }
            }
          }
        }
      }
  }

  val routes = environmentCreationRoute ~ applicationCreationRoute ~ scopeCreationRoute ~ settingUpsertRoute
}
