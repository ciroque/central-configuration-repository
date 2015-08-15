package org.ciroque.ccr.responses

import spray.json.{JsObject, DefaultJsonProtocol}

object HyperMediaResponseProtocol extends DefaultJsonProtocol {
  implicit val HyperMediaMessageResponseFormat = jsonFormat2(HyperMediaMessageResponse)
  implicit val HyperMediaObjectResponseFormat = jsonFormat2(HyperMediaObjectResponse)
}

case class HyperMediaMessageResponse(message: String, _links: Map[String, String])
case class HyperMediaObjectResponse(message: JsObject, _links: Map[String, String])
