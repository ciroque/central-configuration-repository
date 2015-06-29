package org.ciroque.ccr.responses

import spray.json.DefaultJsonProtocol

object ListResponse extends DefaultJsonProtocol {
  implicit val EnvironmentResponseFormat = jsonFormat1(ListResponse.apply)
}

case class ListResponse(environments: List[String])
