package org.ciroque.ccr

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.ciroque.ccr.core.Commons
import org.ciroque.ccr.responses.{RootResponseProtocol, RootResponse}
import spray.http.MediaTypes._
import spray.routing.HttpService
import spray.httpx.SprayJsonSupport.sprayJsonMarshaller

trait ConfigurationProviderService extends HttpService {
  implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)

  def rootRoute = pathEndOrSingleSlash {
    get {
      respondWithMediaType(`application/json`) {
        respondWithHeaders(Commons.corsHeaders) {
          complete {
            import RootResponseProtocol.RootResponseFormat
            RootResponse("Please review the documentation to learn how to use this service.", Map("documentation" -> "/documentation"))
          }
        }
      }
    }
  }

  def routes = rootRoute
}
