package org.ciroque.ccr.responses

import spray.json.{DefaultJsonProtocol, JsObject}

object HyperMediaResponseProtocol extends DefaultJsonProtocol {
  implicit val HyperMediaMessageResponseFormat = jsonFormat2(HyperMediaMessageResponse.apply)
  implicit val HyperMediaObjectResponseFormat = jsonFormat2(HyperMediaObjectResponse)
}

case class HyperMediaMessageResponse(message: String, _links: Map[String, String])

case class HyperMediaObjectResponse(message: JsObject, _links: Map[String, String])

object HyperMediaMessageResponse {
  def apply(message: String) = {
    new HyperMediaMessageResponse(message, Map())
  }
}
