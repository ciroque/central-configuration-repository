package org.ciroque.ccr.responses

import spray.json.DefaultJsonProtocol

object InterstitialGetResponse extends DefaultJsonProtocol {
  implicit val InterstitialGetResponseFormat = jsonFormat1(InterstitialGetResponse.apply)
}

case class InterstitialGetResponse(environments: List[String]) extends CcrResponse
