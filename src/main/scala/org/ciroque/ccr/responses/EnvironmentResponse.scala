package org.ciroque.ccr.responses

import spray.json.DefaultJsonProtocol

object EnvironmentResponse extends DefaultJsonProtocol {
  implicit val EnvironmentResponseFormat = jsonFormat1(EnvironmentResponse.apply)
}

case class EnvironmentResponse(environments: List[String])
