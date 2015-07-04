package org.ciroque.ccr.responses

import spray.json.DefaultJsonProtocol

object InterstitialResponse extends DefaultJsonProtocol {
  implicit val EnvironmentResponseFormat = jsonFormat1(InterstitialResponse.apply)
}

case class InterstitialResponse(environments: List[String]) extends CcrResponse
