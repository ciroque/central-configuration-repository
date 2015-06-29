package org.ciroque.ccr.responses

import spray.json.DefaultJsonProtocol

object RootResponseProtocol extends DefaultJsonProtocol {
  implicit val RootResponseFormat = jsonFormat2(RootResponse.apply)
}

case class RootResponse(message: String, _links: Map[String, String])
