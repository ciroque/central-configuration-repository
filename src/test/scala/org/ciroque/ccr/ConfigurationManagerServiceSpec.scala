package org.ciroque.ccr

import akka.actor.ActorRefFactory
import org.ciroque.ccr.core.Commons
import org.specs2.mutable.Specification
import spray.http.StatusCodes
import spray.testkit.Specs2RouteTest


class ConfigurationManagerServiceSpec
  extends Specification
  with Specs2RouteTest
  with ConfigurationManagerService {

  override def actorRefFactory: ActorRefFactory = system

  override implicit val dataStore: MockSettingsDataStore = new MockSettingsDataStore()

  val managementPath = s"/${Commons.rootPath}/${Commons.managementSegment}"

  val HTTP_CREATED_STATUS = 201

  "ConfigurationManagementService" should {
    "allow adding an environment" in {
      val newEnvironment = "tenv"
      Put(s"$managementPath/$newEnvironment") ~> routes ~> check {
        status must_== StatusCodes.Created
        responseAs[String] must contain(s"/${Commons.rootPath}/${Commons.managementSegment}/$newEnvironment")

      }
    }
    "fail adding an environment" in {
      Put(s"$managementPath/fails") ~> routes ~> check {
        status must_== StatusCodes.UnprocessableEntity
      }
    }
  }
}
