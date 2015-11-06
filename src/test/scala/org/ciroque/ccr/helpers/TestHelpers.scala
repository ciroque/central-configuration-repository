package org.ciroque.ccr.helpers

import org.scalatest.Matchers
import spray.http.HttpHeader
import spray.http.HttpHeaders.RawHeader

/**
  * Created by steve on 11/5/15.
  */
trait TestHelpers extends Matchers {
  protected def assertCorsHeaders(headers: List[HttpHeader]) = {
    headers should contain(RawHeader("Access-Control-Allow-Headers", "Content-Type"))
    headers should contain(RawHeader("Access-Control-Allow-Methods", "GET,PUT,POST"))
    headers should contain(RawHeader("Access-Control-Allow-Origin", "*"))
  }
}
