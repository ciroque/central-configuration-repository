package org.ciroque.ccr

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.ciroque.ccr.core.Commons
import org.ciroque.ccr.datastores.DataStoreResults._
import org.ciroque.ccr.datastores.{CcrTypes, SettingsDataStore}
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.responses.HyperMediaResponseProtocol._
import org.ciroque.ccr.responses._
import org.joda.time.{DateTime, DateTimeZone}
import spray.http.HttpHeaders.RawHeader
import spray.http.MediaTypes._
import spray.http._
import spray.httpx.SprayJsonSupport.sprayJsonMarshaller
import spray.json._
import spray.routing
import spray.routing.{HttpService, RequestContext}
import stats.AccessStatsClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ConfigurationProviderService
  extends HttpService
  with CcrTypes {

  implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)
  implicit val dataStore: SettingsDataStore
  implicit val accessStatsClient: AccessStatsClient

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
        println(s"ConfigurationProviderService::rootRoute")
        accessStatsClient.recordQuery("", "", "", "")
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
          println(s"ConfigurationProviderService::environmentRoute($environment)")
          accessStatsClient.recordQuery(environment, "", "", "")
          completeRoute[String](
            ctx,
            dataStore.retrieveApplications(environment),
            list => (ApplicationGetResponse(list).toJson, StatusCodes.OK)
          )
        }
      }
  }

  def applicationsRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment) {
    (environment, application) =>
      pathEndOrSingleSlash {
        get { ctx =>
          println(s"ConfigurationProviderService::applicationsRoute")
          accessStatsClient.recordQuery(environment, application, "", "")
          completeRoute[String](
            ctx,
            dataStore.retrieveScopes(environment, application),
            list => (ScopeGetResponse(list).toJson, StatusCodes.OK)
          )
        }
      }
  }

  def scopeRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment / Segment) {
    (environment, application, scope) =>
      pathEndOrSingleSlash {
        get { ctx =>
          println(s"ConfigurationProviderService::scopeRoute")
          accessStatsClient.recordQuery(environment, application, scope, "")
          completeRoute[String](
            ctx,
            dataStore.retrieveSettings(environment, application, scope),
            list => (SettingGetResponse(list).toJson, StatusCodes.OK)
          )
        }
      }
  }

  def settingRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment / Segment / Segment) {
    (environment, application, scope, setting) =>
      pathEndOrSingleSlash {
        get { ctxt =>
          println(s"ConfigurationProviderService::settingRoute")
          accessStatsClient.recordQuery(environment, application, scope, setting)
          import org.ciroque.ccr.responses.ConfigurationResponseProtocol._

          def buildHeaders(configurations: List[ConfigurationFactory.Configuration]) = {
            configurations match {
              case Nil => Commons.corsHeaders
              case _ => Commons.corsHeaders :+ RawHeader("Expires", DateTime.now(DateTimeZone.UTC).plusMillis(configurations.head.temporality.ttl.toInt).toString())
            }
          }

          completeRoute[ConfigurationFactory.Configuration](
            ctxt,
            dataStore.retrieveConfiguration(environment, application, scope, setting),
            list => (ConfigurationResponse(list).toJson, StatusCodes.OK),
            hyperMediaResponseFactory,
            Commons.failureResponseFactory,
            dsr => buildHeaders(dsr)
          )
        }
      }
  }

  def routes = rootRoute ~ defaultRoute ~ appRoute ~ environmentRoute ~ applicationsRoute ~ scopeRoute ~ settingRoute

  private def completeRoute[T](context: RequestContext,
                               eventualDataStoreResult: Future[DataStoreResult],
                               foundFactory: List[T] => (JsValue, StatusCode),
                               notFoundFactory: (String) => (JsValue, StatusCode) = hyperMediaResponseFactory,
                               failureFactory: (String, Throwable) => (JsValue, StatusCode) = Commons.failureResponseFactory,
                               generateHeader: (List[T]) => List[HttpHeaders.RawHeader] = (items: List[T]) => Commons.corsHeaders) = {

    for {
      entities <- eventualDataStoreResult
    } yield {
      val ((jsonResult, statusCode), listOfEntities) = entities match {
        case Found(items: List[T]) => (foundFactory(items), items)
        case NotFound(message) => (notFoundFactory(message), List())
        case Failure(message, cause) => (failureFactory(message, cause), List())
      }

      context.complete(HttpResponse(
        statusCode,
        HttpEntity(`application/json`, jsonResult.toString()),
        generateHeader(listOfEntities)))
    }
  }

  private def hyperMediaResponseFactory(message: String): (JsValue, StatusCode) =
    (new HyperMediaMessageResponse(message, Map()).toJson, StatusCodes.NotFound)

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
