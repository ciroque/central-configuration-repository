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

import scala.concurrent.Future

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
        import org.ciroque.ccr.responses.EnvironmentGetResponseProtocol._
        println(s"ConfigurationProviderService::rootRLoute")
        completeRoute[String](
          ctx,
          dataStore.retrieveEnvironments(),
          list => (EnvironmentGetResponse(list).toJson, StatusCodes.OK))
      }
    }
  }

  def environmentRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment) {
    environment =>
      pathEndOrSingleSlash {
        get { ctx =>
          import org.ciroque.ccr.responses.ApplicationGetResponseProtocol._
          println(s"ConfigurationProviderService::environmentRoute")
          completeRoute[String](
            ctx,
            dataStore.retrieveApplications(environment),
            list => (ApplicationGetResponse(list).toJson, StatusCodes.OK))
        }
      }
  }

  def applicationsRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment) {
    (environment, application) =>
      pathEndOrSingleSlash {
        get { ctx =>
          println(s"ConfigurationProviderService::applicationsRoute")
          completeRoute[String](
            ctx,
            dataStore.retrieveScopes(environment, application),
            list => (ScopeGetResponse(list).toJson, StatusCodes.OK))
        }
      }
  }

  def scopeRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment / Segment) {
    (environment, application, scope) =>
      pathEndOrSingleSlash {
        get { ctx =>
          println(s"ConfigurationProviderService::scopeRoute")
          completeRoute[String](
            ctx,
            dataStore.retrieveSettings(environment, application, scope),
            list => (SettingGetResponse(list).toJson, StatusCodes.OK))
        }
      }
  }

  def settingRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment / Segment / Segment) {
    (environment, application, scope, setting) =>
      pathEndOrSingleSlash {
        get { ctxt =>
          println(s"ConfigurationProviderService::settingRoute")

          import org.ciroque.ccr.responses.ConfigurationResponseProtocol._
          completeRoute[ConfigurationFactory.Configuration](
            ctxt,
            dataStore.retrieveConfiguration(environment, application, scope, setting),
            list => (ConfigurationResponse(list).toJson, StatusCodes.OK)
          )
        }
      }
  }

  def routes = rootRoute ~ defaultRoute ~ appRoute ~ environmentRoute ~ applicationsRoute ~ scopeRoute ~ settingRoute

  private def completeRoute[T](context: RequestContext,
                               eventualDataStoreResult: Future[DataStoreResult],
                               foundFactory: List[T] => (JsValue, StatusCode),
                               notFoundFactory: (String, String) => (JsValue, StatusCode) = hyperMediaResponseFactory,
                               failureFactory: (String, Throwable) => (JsValue, StatusCode) = failureResponseFactory) = {

    for {
      entities <- eventualDataStoreResult
    } yield {
      val (result, statusCode) = entities match {
        case Found(items: List[T]) => foundFactory(items)
        case NotFound(key, value) => notFoundFactory(key, value)
        case NoChildrenFound(key, value) => foundFactory(List())
        case Failure(message, cause) => failureFactory(message, cause)
      }

      context.complete(HttpResponse(statusCode, HttpEntity(`application/json`, result.toString()), Commons.corsHeaders))
    }
  }

  private def failureResponseFactory(message: String, cause: Throwable): (JsValue, StatusCode) = {
    import org.ciroque.ccr.responses.InternalServerErrorResponseProtocol._
    (InternalServerErrorResponse(message, cause.getMessage).toJson, StatusCodes.InternalServerError)
  }

  private def hyperMediaResponseFactory(key: String, value: String): (JsValue, StatusCode) =
    (new HyperMediaMessageResponse(s"$key '$value' was not found.", Map()).toJson, StatusCodes.NotFound)

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
