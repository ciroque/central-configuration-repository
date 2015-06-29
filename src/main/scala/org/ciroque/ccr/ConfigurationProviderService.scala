package org.ciroque.ccr

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.ciroque.ccr.core.Utils
import spray.http.MediaTypes._
import spray.routing.HttpService

trait ConfigurationProviderService extends HttpService {
  implicit val timeout: Timeout = Timeout(3, TimeUnit.SECONDS)

  def rootRoute = pathEndOrSingleSlash {
    get {
      respondWithMediaType(`application/json`) {
        respondWithHeaders(Utils.corsHeaders) {
          complete {
            "This is not the endpoint you are looking for..."
          }
        }
      }
    }
  }

  def routes = rootRoute
}
