package com.example

import akka.actor.ActorRefFactory
import org.ciroque.ccr.ConfigurationProviderService
import org.specs2.mutable.Specification
import spray.http.HttpHeaders.RawHeader
import spray.http._
import spray.testkit.Specs2RouteTest

class ConfigurationProviderServiceSpec
  extends Specification
  with Specs2RouteTest
  with ConfigurationProviderService {

  def assertCorsHeaders(headers: List[HttpHeader]) = {
    headers.contains(RawHeader("Access-Control-Allow-Headers", "Content-Type"))
    headers.contains(RawHeader("Access-Control-Allow-Methods", "GET"))
    headers.contains(RawHeader("Access-Control-Allow-Origin", "*"))
  }

  def actorRefFactory: ActorRefFactory = system

  "ConfigurationProviderService" should {
    "return a silly message on the root route" in {
      Get("/") ~> routes ~> check {
        status.intValue must_== 200
        assertCorsHeaders(headers)
      }
    }
  }
}
