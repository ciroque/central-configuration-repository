package org.ciroque.ccr.core

import spray.http.HttpHeaders.RawHeader

object Commons {
  val rootPath = "ccr"

  val settingsSegment = "settings"
  val managementSegment = "management"

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

