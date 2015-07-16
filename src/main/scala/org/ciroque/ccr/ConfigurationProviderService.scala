package org.ciroque.ccr

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.ciroque.ccr.core.{CcrTypes, Commons, SettingsDataStore}
import org.ciroque.ccr.responses._
import spray.http.MediaTypes._
import spray.http.{HttpEntity, HttpResponse, StatusCodes}
import spray.httpx.SprayJsonSupport.sprayJsonMarshaller
import spray.json._
import spray.routing
import spray.routing.{HttpService, RequestContext}

trait ConfigurationProviderService
  extends HttpService
  with CcrTypes {

  implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)
  implicit val dataStore: SettingsDataStore

  private def completeInterstitialRoute(context: RequestContext,
                                        entities: Option[List[String]],
                                        notFoundMessage: String,
                                        factory: List[String] => JsValue) = {

    val (result, statusCode) = entities match {
      case Some(list) => (list, StatusCodes.OK)
      case None => (List(notFoundMessage), StatusCodes.NotFound)
    }

    context.complete(HttpResponse(statusCode, HttpEntity(`application/json`, factory(result).toString()), Commons.corsHeaders))
  }

  def defaultRoute = pathEndOrSingleSlash {
    get {
      respondWithTeapot
    }
  }

  def appRoute = pathPrefix(Commons.rootPath) {
    pathEndOrSingleSlash {
      get {
        respondWithTeapot
      }
    }
  }

  def rootRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment) {
    pathEndOrSingleSlash {
      get { ctx =>
        completeInterstitialRoute(ctx, dataStore.retrieveEnvironments(), "No environments found.", list => EnvironmentGetResponse(list).toJson)
      }
    }
  }

  def environmentRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment) {
    environment =>
      pathEndOrSingleSlash {
        get { ctx =>
          println(s"ConfigurationProviderService::environmentRoute")
          completeInterstitialRoute(
            ctx,
            dataStore.retrieveApplications(environment),
            s"environment '$environment' was not found",
            list => ApplicationGetResponse(list).toJson)
        }
      }
  }

  def applicationsRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment) {
    (environment, application) =>
      pathEndOrSingleSlash {
        get { ctx =>
          println(s"ConfigurationProviderService::applicationsRoute")
          completeInterstitialRoute(
            ctx,
            dataStore.retrieveScopes(environment, application),
            s"application '$application' in environment '$environment' was not found",
            list => ScopeGetResponse(list).toJson)
        }
      }
  }

  def scopeRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment / Segment) {
    (environment, application, scope) =>
      pathEndOrSingleSlash {
        get { ctx =>
          println(s"ConfigurationProviderService::scopeRoute")
          completeInterstitialRoute(
            ctx,
            dataStore.retrieveSettings(environment, application, scope),
            s"scope '$scope' for application '$application' in environment '$environment' was not found",
            list => SettingGetResponse(list).toJson
          )
        }
      }
  }

  def settingRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment / Segment / Segment) {
    (environment, application, scope, setting) =>
      pathEndOrSingleSlash {
        get {
          println(s"ConfigurationProviderService::settingRoute")
          respondWithMediaType(`application/json`) {
            respondWithHeaders(Commons.corsHeaders) {
              dataStore.retrieveConfiguration(environment, application, scope, setting) match {
                case Some(_) => complete {
                  SettingResponse(dataStore.retrieveConfiguration(environment, application, scope, setting))
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

  def routes = defaultRoute ~ appRoute ~ environmentRoute ~ applicationsRoute ~ scopeRoute ~ settingRoute ~ rootRoute

  private def respondWithTeapot: routing.Route = {
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
