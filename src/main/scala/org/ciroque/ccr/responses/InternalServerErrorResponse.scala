package org.ciroque.ccr.responses

import spray.json._

object InternalServerErrorResponseProtocol extends DefaultJsonProtocol {
  implicit val internalServerErrorFormat = jsonFormat2(InternalServerErrorResponse)
}

case class InternalServerErrorResponse(message: String, cause: String) extends CcrResponse
