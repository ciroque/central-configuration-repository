package org.ciroque.ccr.responses

import spray.json.DefaultJsonProtocol

object InterstitialPutResponse extends DefaultJsonProtocol {
  implicit val InterstitialPutResponseFormat = jsonFormat1(InterstitialPutResponse.apply)
}

case class InterstitialPutResponse(entityPath: String)
