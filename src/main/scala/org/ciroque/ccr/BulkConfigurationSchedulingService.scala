package org.ciroque.ccr

import scala.concurrent.ExecutionContext.Implicits.global
import org.ciroque.ccr.core.{Commons, CcrService}
import org.ciroque.ccr.datastores.DataStoreResults._
import org.ciroque.ccr.datastores.SettingsDataStore
import org.ciroque.ccr.logging.ImplicitLogging._
import org.ciroque.ccr.models.ConfigurationFactory._
import org.ciroque.ccr.responses.{BulkConfigurationStatusFactory, BulkConfigurationResponse, BulkConfigurationStatus}
import org.ciroque.ccr.responses.BulkConfigurationResponseProtocol._
import org.slf4j.Logger
import spray.http.MediaTypes._
import spray.http.{StatusCode, HttpEntity, StatusCodes, HttpResponse}
import spray.routing.{RequestContext, HttpService}
import spray.httpx.SprayJsonSupport._
import spray.json._

import scala.concurrent.Future

trait BulkConfigurationSchedulingService
  extends HttpService
  with CcrService {

  implicit val dataStore: SettingsDataStore
  implicit val logger: Logger

  override def getVersion = new SemanticVersion(1, 0, 0)

  def bulkRoute = path(Commons.rootPath / Commons.schedulingSegment / Commons.bulkSegment) {
    pathEndOrSingleSlash {
      entity(as[ConfigurationList]) {
        configurations ⇒
          post { context ⇒
            withImplicitLogging("ConfigurationBulkSchedulingService::POST") {
              recordValue("configurations", configurations.toJson.toString())
              val dsr = dataStore.bulkInsertConfigurations(configurations)
              val bulkConfigurationResponse = processDataStoreResult(dsr)
              respond(context, bulkConfigurationResponse)
            }
          } ~
          put { context ⇒
            withImplicitLogging("ConfigurationBulkSchedulingService::PUT") {
              recordValue("configurations", configurations.toJson.toString())
              val dsr = dataStore.bulkUpdateConfigurations(configurations)
              val bulkConfigurationResponse = processDataStoreResult(dsr)
              respond(context, bulkConfigurationResponse)
            }
          }
      }
    }
  }

  private def respond(context: RequestContext, response: Future[BulkConfigurationResponse]) = {
    for {
      eventualResult ← response
    } yield {
      val rval = eventualResult.toJson.toString()
      recordValue("result", rval)
      context.complete(HttpResponse(
        StatusCode.int2StatusCode(eventualResult.getStatusCode),
        HttpEntity(`application/json`, eventualResult.toJson.toString()),
        Commons.corsHeaders))
    }
  }

  private def processDataStoreResult(dsr: Future[List[DataStoreResult]]): Future[BulkConfigurationResponse] = {
    dsr.map {
      dataStoreResults ⇒
        val bulkConfigurationStatuses = dataStoreResults.map {
          case Added(config: Configuration) ⇒
            BulkConfigurationStatusFactory(StatusCodes.Created.intValue, config)

          case Updated(prevItem: Configuration, newItem: Configuration) ⇒
            BulkConfigurationStatusFactory(StatusCodes.OK.intValue, prevItem, newItem)

          case Errored(item: Configuration, msg: String) ⇒
            BulkConfigurationStatusFactory(StatusCodes.UnprocessableEntity.intValue, item, msg)

          case Failure(msg: String, cause: Throwable) ⇒
            BulkConfigurationStatusFactory(StatusCodes.InternalServerError.intValue, msg)

          case _ =>
            BulkConfigurationStatusFactory(StatusCodes.Unauthorized.intValue, s"$dataStoreResults")
        }

        BulkConfigurationResponse(bulkConfigurationStatuses)
    }.recoverWith { case t: Throwable => Future.successful(BulkConfigurationResponse.FailedBulkConfigurationResponse(t.getMessage)) }
  }

  def routes = bulkRoute
}
