package org.ciroque.ccr

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import com.wordnik.swagger.annotations.{Api, ApiImplicitParam, ApiImplicitParams, ApiOperation}
import org.ciroque.ccr.core.{CcrService, Commons}
import org.ciroque.ccr.datastores.DataStoreResults._
import org.ciroque.ccr.datastores.{CcrTypes, SettingsDataStore}
import org.ciroque.ccr.logging.ImplicitLogging._
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.responses.HyperMediaResponseProtocol._
import org.ciroque.ccr.responses._
import org.ciroque.ccr.stats.AccessStatsClient
import org.joda.time.{DateTime, DateTimeZone}
import org.slf4j.Logger
import spray.http.HttpHeaders.RawHeader
import spray.http.MediaTypes._
import spray.http._
import spray.httpx.SprayJsonSupport.sprayJsonMarshaller
import spray.json._
import spray.routing
import spray.routing.{HttpService, RequestContext}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Api(value = Commons.ApiDocumentationStrings.ApiRoute, description = Commons.ApiDocumentationStrings.ConfigurationProviderApiDescription)
trait ConfigurationProviderService
  extends HttpService
  with CcrService
  with CcrTypes {

  implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)
  implicit val dataStore: SettingsDataStore
  implicit val accessStatsClient: AccessStatsClient
  implicit val logger: Logger

  override def getVersion = new SemanticVersion(1, 0, 0)

  def routes = defaultRoute ~ appRoute ~ environmentsRoute ~ applicationsRoute ~ scopesRoute ~ settingsRoute ~ configurationRoute

  @ApiOperation(
    value = Commons.ApiDocumentationStrings.RootRoute,
    notes = Commons.ApiDocumentationStrings.SeeDocumentation,
    httpMethod = Commons.ApiDocumentationStrings.GetMethod,
    nickname = Commons.ApiDocumentationStrings.RootRoute)
  def defaultRoute = pathEndOrSingleSlash {
    get {
      respondWithTeapot
    }
  }

  @ApiOperation(
    value = Commons.ApiDocumentationStrings.AppRoute,
    notes = Commons.ApiDocumentationStrings.SeeDocumentation,
    httpMethod = Commons.ApiDocumentationStrings.GetMethod,
    nickname = Commons.ApiDocumentationStrings.AppRoute)
  def appRoute = path(Commons.rootPath) {
    pathEndOrSingleSlash {
      get {
        respondWithTeapot
      }
    }
  }

  private def respondWithTeapot: routing.Route = {
    respondWithMediaType(`application/json`) {
      respondWithHeaders(Commons.corsHeaders) {
        respondWithStatus(Commons.teaPotStatusCode) {
          complete {
            HyperMediaMessageResponse(
              Commons.ApiDocumentationStrings.SeeDocumentation,
              Map(
                "documentation" -> "/documentation",
                "service" -> s"${Commons.rootPath}/${Commons.settingsSegment}"))
          }
        }
      }
    }
  }

  @ApiOperation(
    value = Commons.ApiDocumentationStrings.EnvironmentsRoute,
    notes = Commons.ApiDocumentationStrings.EnvironmentsNotes,
    httpMethod = Commons.ApiDocumentationStrings.GetMethod,
    nickname = Commons.ApiDocumentationStrings.EnvironmentsRoute)
  def environmentsRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment) {
    pathEndOrSingleSlash {
      get { ctx =>
        import org.ciroque.ccr.responses.EnvironmentGetResponseProtocol._
        withImplicitLoggingAndStats("ConfigurationProviderService::environmentsRoute", "", "", "", "") {
          completeRoute[String](
            ctx,
            dataStore.retrieveEnvironments(),
            list => (EnvironmentGetResponse(list).toJson, StatusCodes.OK))
        }
      }
    }
  }

  @ApiOperation(
    value = Commons.ApiDocumentationStrings.ApplicationsRoute,
    notes = Commons.ApiDocumentationStrings.ApplicationsNotes,
    httpMethod = Commons.ApiDocumentationStrings.GetMethod,
    nickname = Commons.ApiDocumentationStrings.ApplicationsRoute)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      name = Commons.KeyStrings.EnvironmentKey,
      dataType = Commons.ApiDocumentationStrings.StringDataType,
      required = true,
      paramType = Commons.ApiDocumentationStrings.PathParamType)))
  def applicationsRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment) {
    environment =>
      pathEndOrSingleSlash {
        get { ctx =>
          import org.ciroque.ccr.responses.ApplicationGetResponseProtocol._
          withImplicitLoggingAndStats("ConfigurationProviderService::applicationsRoute", environment, "", "", "") {
            completeRoute[String](
              ctx,
              dataStore.retrieveApplications(environment),
              list => (ApplicationGetResponse(environment, list).toJson, StatusCodes.OK)
            )
          }
        }
      }
  }

  @ApiOperation(
    value = Commons.ApiDocumentationStrings.ScopesRoute,
    notes = Commons.ApiDocumentationStrings.ScopesNotes,
    httpMethod = Commons.ApiDocumentationStrings.GetMethod,
    nickname = Commons.ApiDocumentationStrings.ScopesRoute)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      name = Commons.KeyStrings.ApplicationKey,
      dataType = Commons.ApiDocumentationStrings.StringDataType,
      required = true,
      paramType = Commons.ApiDocumentationStrings.StringDataType),
    new ApiImplicitParam(
      name = Commons.KeyStrings.EnvironmentKey,
      dataType = Commons.ApiDocumentationStrings.StringDataType,
      required = true,
      paramType = Commons.ApiDocumentationStrings.PathParamType)
  ))
  def scopesRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment) {
    (environment, application) =>
      pathEndOrSingleSlash {
        get { ctx =>
          withImplicitLoggingAndStats("ConfigurationProviderService::scopesRoute", environment, application, "", "") {
            completeRoute[String](
              ctx,
              dataStore.retrieveScopes(environment, application),
              list => (ScopeGetResponse(environment, application, list).toJson, StatusCodes.OK)
            )
          }
        }
      }
  }

  @ApiOperation(
    value = Commons.ApiDocumentationStrings.SettingsRoute,
    notes = Commons.ApiDocumentationStrings.SettingsNotes,
    httpMethod = Commons.ApiDocumentationStrings.SettingsNotes,
    nickname = Commons.ApiDocumentationStrings.SettingsRoute)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      name = Commons.KeyStrings.EnvironmentKey,
      dataType = Commons.ApiDocumentationStrings.StringDataType,
      required = true,
      paramType = Commons.ApiDocumentationStrings.PathParamType),
    new ApiImplicitParam(
      name = Commons.KeyStrings.ApplicationKey,
      dataType = Commons.ApiDocumentationStrings.StringDataType,
      required = true,
      paramType = Commons.ApiDocumentationStrings.PathParamType),
    new ApiImplicitParam(
      name = Commons.KeyStrings.ScopeKey,
      dataType = Commons.ApiDocumentationStrings.StringDataType,
      required = true,
      paramType = Commons.ApiDocumentationStrings.PathParamType)
  ))
  def settingsRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment / Segment) {
    (environment, application, scope) =>
      pathEndOrSingleSlash {
        get { ctx =>
          withImplicitLoggingAndStats("ConfigurationProviderService::settingsRoute", environment, application, scope, "") {
            completeRoute[String](
              ctx,
              dataStore.retrieveSettings(environment, application, scope),
              list => (SettingGetResponse(environment, application, scope, list).toJson, StatusCodes.OK)
            )
          }
        }
      }
  }

  private def completeRoute[T](context: RequestContext,
                               eventualDataStoreResult: Future[DataStoreResult],
                               foundFactory: List[T] => (JsValue, StatusCode),
                               notFoundFactory: (String) => (JsValue, StatusCode) = hyperMediaResponseFactory,
                               failureFactory: (String, Throwable) => (JsValue, StatusCode) = Commons.failureResponseFactory,
                               generateHeaders: (List[T]) => List[HttpHeaders.RawHeader] = (items: List[T]) => Commons.corsHeaders) = {

    for {
      entities <- eventualDataStoreResult
    } yield {
      val ((jsonResult, statusCode), listOfEntities) = entities match {
        case Found(items: List[T]) => (foundFactory(items), items)
        case NotFound(message) => (notFoundFactory(message), List())
        case Failure(message, cause) => (failureFactory(message, cause), List())
        case _ => ((s"No match for entities. ${entities.toString}", StatusCodes.InternalServerError), List())
      }

      context.complete(HttpResponse(
        statusCode,
        HttpEntity(`application/json`, jsonResult.toString()),
        generateHeaders(listOfEntities)))
    }
  }

  private def withImplicitLoggingAndStats[T](name: String,
                                             environment: String,
                                             application: String,
                                             scope: String,
                                             setting: String)(fx: => T) = {

    accessStatsClient.recordQuery(environment, application, scope, setting)
    val values = Map(Commons.KeyStrings.EnvironmentKey -> environment, Commons.KeyStrings.ApplicationKey -> application, Commons.KeyStrings.ScopeKey -> scope, Commons.KeyStrings.SettingKey -> setting)

    withImplicitLogging(name) {
      values.filter(thing => thing._2 != "").map(thing => recordValue(thing._1, thing._2))
      fx
    }
  }

  @ApiOperation(
    value = Commons.ApiDocumentationStrings.ConfigurationsRoute,
    notes = Commons.ApiDocumentationStrings.ConfigurationsNotes,
    httpMethod = Commons.ApiDocumentationStrings.GetMethod,
    nickname = Commons.ApiDocumentationStrings.ConfigurationsRoute)
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      name = Commons.KeyStrings.EnvironmentKey,
      dataType = Commons.ApiDocumentationStrings.StringDataType,
      required = true,
      paramType = Commons.ApiDocumentationStrings.PathParamType),
    new ApiImplicitParam(
      name = Commons.KeyStrings.ApplicationKey,
      dataType = Commons.ApiDocumentationStrings.StringDataType,
      required = true,
      paramType = Commons.ApiDocumentationStrings.PathParamType),
    new ApiImplicitParam(
      name = Commons.KeyStrings.ScopeKey,
      dataType = Commons.ApiDocumentationStrings.StringDataType,
      required = true,
      paramType = Commons.ApiDocumentationStrings.PathParamType),
    new ApiImplicitParam(
      name = Commons.KeyStrings.SettingKey,
      dataType = Commons.ApiDocumentationStrings.StringDataType,
      required = true,
      paramType = Commons.ApiDocumentationStrings.PathParamType)
  ))
  def configurationRoute = pathPrefix(Commons.rootPath / Commons.settingsSegment / Segment / Segment / Segment / Segment) {
    (environment, application, scope, setting) ⇒
      parameter('sourceId ?) { sourceId ⇒
        pathEndOrSingleSlash {
          get { ctxt =>

            def buildHeaders(configurations: List[ConfigurationFactory.Configuration]) = {
              import spray.http.CacheDirectives.`max-age`
              import spray.http.HttpHeaders.`Cache-Control`

              configurations match {
                case Nil => Commons.corsHeaders
                case _ =>

                  val maxAgeHeader = `Cache-Control`(`max-age`(configurations.head.temporality.ttl))
                  val expiry = DateTime.now(DateTimeZone.UTC).plusSeconds(configurations.head.temporality.ttl.toInt)
                  val expiry1123 = Commons.DateTimeFormatter1123.print(expiry)
                  val expiresHeader = RawHeader("Expires", expiry1123)
                  Commons.corsHeaders :+ expiresHeader :+ RawHeader(maxAgeHeader.name, maxAgeHeader.value)
              }
            }

            withImplicitLoggingAndStats("ConfigurationProviderService::configurationRoute", environment, application, scope, setting) {
              import org.ciroque.ccr.responses.ConfigurationResponseProtocol._

              completeRoute[ConfigurationFactory.Configuration](
                ctxt,
                dataStore.retrieveConfiguration(environment, application, scope, setting, sourceId),
                list => (ConfigurationResponse(list).toJson, StatusCodes.OK),
                hyperMediaResponseFactory,
                Commons.failureResponseFactory,
                dsr => buildHeaders(dsr)
              )
            }
          }
        }
      }
  }

  private def hyperMediaResponseFactory(message: String): (JsValue, StatusCode) =
    (new HyperMediaMessageResponse(message, Map()).toJson, StatusCodes.NotFound)
}
