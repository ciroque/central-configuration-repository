package org.ciroque.ccr.core

import org.ciroque.ccr.responses.InternalServerErrorResponse
import spray.http.HttpHeaders.RawHeader
import spray.http.{StatusCode, StatusCodes}
import spray.json._

object Commons {
  val rootPath = "ccr"

  object KeyStrings {
    val actorSystemName = "central-configuration-repository-system"
    val idKey = "_id"
    val environmentKey = "environment"
    val applicationKey = "application"
    val scopeKey = "scope"
    val settingKey = "setting"
    val effectiveAtKey = "effectiveAt"
    val expiresAtKey = "expiresAt"
    val ttlKey = "ttl"
    val temporalityKey = "temporality"
    val temporalizationKey = "temporalization"
    val keyKey = "key"
    val valueKey = "value"
  }

  val settingsSegment = "settings"
  val settingSegment = "setting"
  val managementSegment = "schedule"

  val teaPotStatusCode = StatusCodes.registerCustom(418, "I'm a tea pot")

  val corsHeaders = List(
    RawHeader("Access-Control-Allow-Origin", "*"),
    RawHeader("Access-Control-Allow-Headers", "Content-Type"),
    RawHeader("Access-Control-Allow-Methods", "GET,PUT")
  )

  def failureResponseFactory(message: String, cause: Throwable): (JsValue, StatusCode) = {
    import org.ciroque.ccr.responses.InternalServerErrorResponseProtocol._
    (InternalServerErrorResponse(message, cause.getMessage).toJson, StatusCodes.InternalServerError)
  }
}
