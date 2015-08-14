package org.ciroque.ccr

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.ciroque.ccr.core.DataStoreResults._
import org.ciroque.ccr.core.{CcrTypes, Commons, SettingsDataStore}
import org.ciroque.ccr.models.ConfigurationFactory
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
                                        entities: DataStoreResult,
                                        notFoundMessage: String,
                                        factory: List[String] => JsValue) = {

    val (result, statusCode) = entities match {
      case Found(items: List[String]) => (factory(items).toString(), StatusCodes.OK)
      case NotFound(key) => (JsString(s"No children of $key found.").toString(), StatusCodes.NotFound)
      case Failure(message, cause) => (JsString("Something went horribly, horribly wrong.").toString(), StatusCodes.InternalServerError)

      case Success() => throw new UnsupportedOperationException("Success class is deprecated.")
    }

    context.complete(HttpResponse(statusCode, HttpEntity(`application/json`, result), Commons.corsHeaders))
  }

  def defaultRoute = pathEndOrSingleSlash {
    get {
      respondWithTeapot
    }
  }

  def appRoute = path(Commons.rootPath) {
    pathEndOrSingleSlash {
      get {
        respondWithTeapot
      }
    }
  }

  def rootRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment) {
    pathEndOrSingleSlash {
      get { ctx =>
        import scala.concurrent.ExecutionContext.Implicits.global
        for {
          result <- dataStore.retrieveEnvironments()
        } yield {
          completeInterstitialRoute(ctx, result, "No environments found.", list => EnvironmentGetResponse(list).toJson)
        }
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
            s"environment '$environment' has no applications defined.",
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
            s"application '$application' in environment '$environment' has no scopes defined.",
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
            s"scope '$scope' for application '$application' in environment '$environment' has no settings defined.",
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

                case Found(list: List[ConfigurationFactory.Configuration]) => complete { SettingResponse(list) }
                case NotFound(key: String) =>  respondWithStatus(StatusCodes.NotFound) {
                  complete {
                    s"setting '$setting' in scope '$scope' for application '$application' in environment '$environment' was not found"
                  }
                }
                case Failure(msg, cause) => respondWithStatus(StatusCodes.InternalServerError) {
                  complete {
                    s"Something really bad happened."
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
