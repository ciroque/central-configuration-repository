package org.ciroque.ccr.core

import spray.http.HttpHeaders.RawHeader
import spray.http.StatusCodes

object Commons {
  val rootPath = "ccr"

  val settingsSegment = "settings"
  val managementSegment = "management"

  val teaPotStatusCode = StatusCodes.registerCustom(418, "I'm a tea pot")

  val corsHeaders = List(
    RawHeader("Access-Control-Allow-Origin", "*"),
    RawHeader("Access-Control-Allow-Headers", "Content-Type"),
    RawHeader("Access-Control-Allow-Methods", "GET")
  )
}

object DataStoreResults {
  trait DataStoreResult
  case class Success() extends DataStoreResult
  case class Failure(message: String, cause: Throwable = null) extends DataStoreResult
}

