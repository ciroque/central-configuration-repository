package org.ciroque.ccr

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.ciroque.ccr.core.DataStoreResults._
import org.ciroque.ccr.core.{CcrTypes, Commons, SettingsDataStore}
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.responses._
import spray.http.MediaTypes._
import spray.http.{StatusCode, HttpEntity, HttpResponse, StatusCodes}
import spray.httpx.SprayJsonSupport.sprayJsonMarshaller
import spray.json._
import spray.routing
import spray.routing.{HttpService, RequestContext}
import scala.concurrent.ExecutionContext.Implicits.global
import org.ciroque.ccr.responses.HyperMediaResponseProtocol._

trait ConfigurationProviderService
  extends HttpService
  with CcrTypes {

  implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)
  implicit val dataStore: SettingsDataStore

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
        for {
          result <- dataStore.retrieveEnvironments()
        } yield {
          import org.ciroque.ccr.responses.EnvironmentGetResponseProtocol._
          completeInterstitialRoute(ctx, result, list => EnvironmentGetResponse(list).toJson)
        }
      }
    }
  }

  def environmentRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment) {
    environment =>
      pathEndOrSingleSlash {
        get { ctx =>
          import org.ciroque.ccr.responses.ApplicationGetResponseProtocol._
          println(s"ConfigurationProviderService::environmentRoute")
          for {
            result <- dataStore.retrieveApplications(environment)
          } yield {
            completeInterstitialRoute(
              ctx,
              result,
              list => ApplicationGetResponse(list).toJson)
          }
        }
      }
  }

  def applicationsRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment) {
    (environment, application) =>
      pathEndOrSingleSlash {
        get { ctx =>
          println(s"ConfigurationProviderService::applicationsRoute")
          for {
            result <- dataStore.retrieveScopes(environment, application)
          } yield {
            completeInterstitialRoute(
              ctx,
              result,
              list => ScopeGetResponse(list).toJson)
          }
        }
      }
  }

  def scopeRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment / Segment) {
    (environment, application, scope) =>
      pathEndOrSingleSlash {
        get { ctx =>
          println(s"ConfigurationProviderService::scopeRoute")
          for {
            result <- dataStore.retrieveSettings(environment, application, scope)
          } yield {
            completeInterstitialRoute(
              ctx,
              result,
              list => SettingGetResponse(list).toJson
            )
          }
        }
      }
  }

  def settingRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment / Segment / Segment) {


    (environment, application, scope, setting) =>
      pathEndOrSingleSlash {
        get { context =>
          println(s"ConfigurationProviderService::settingRoute")
          import org.ciroque.ccr.responses.ConfigurationResponseProtocol._
          for {
            dsr <- dataStore.retrieveConfiguration(environment, application, scope, setting)
          } yield {
            val (result, statusCode) = dsr match {
              case Found(list: List[ConfigurationFactory.Configuration]) => (ConfigurationResponse(list).toJson.toString(), StatusCodes.OK)
              case NotFound(key: String, value: String) =>
                (new HyperMediaMessageResponse(
                  s"setting '$setting' in scope '$scope' for application '$application' in environment '$environment' was not found", Map()).toJson.toString(),
                  StatusCodes.NotFound)
              case Failure(msg, cause) => {
                (JsString(s"Something really bad happened.").toString(), StatusCodes.InternalServerError)
              }
            }

            context.complete(HttpResponse(statusCode, HttpEntity(`application/json`, result), Commons.corsHeaders))
          }

        }
      }
  }

  def routes = defaultRoute ~ appRoute ~ environmentRoute ~ applicationsRoute ~ scopeRoute ~ settingRoute ~ rootRoute

  private def completeInterstitialRoute(context: RequestContext,
                                        entities: DataStoreResult,
                                        factory: List[String] => JsValue) = {

    val (result, statusCode) = entities match {
      case Found(items: List[String]) => (factory(items).toString(), StatusCodes.OK)
      case NotFound(key, value) => (new HyperMediaMessageResponse(s"$key '$value' was not found.", Map()).toJson.toString(), StatusCodes.NotFound)
      case NoChildrenFound(key, value) => (factory(List()).toString(), StatusCodes.OK)
      case Failure(message, cause) => (JsString("Something went horribly, horribly wrong.").toString(), StatusCodes.InternalServerError)
    }

    context.complete(HttpResponse(statusCode, HttpEntity(`application/json`, result), Commons.corsHeaders))
  }

  private def respondWithTeapot: routing.Route = {
    respondWithMediaType(`application/json`) {
      respondWithHeaders(Commons.corsHeaders) {
        respondWithStatus(Commons.teaPotStatusCode) {
          complete {

            import org.ciroque.ccr.responses.HyperMediaResponseProtocol._

            HyperMediaMessageResponse("Please review the documentation to learn how to use this service.", Map("documentation" -> "/documentation"))
          }
        }
      }
    }
  }
}
