package org.ciroque.ccr.core

import spray.http.HttpHeaders.RawHeader

object Commons {
  val corsHeaders = List(
    RawHeader("Access-Control-Allow-Origin", "*"),
    RawHeader("Access-Control-Allow-Headers", "Content-Type"),
    RawHeader("Access-Control-Allow-Methods", "GET")
  )
}
