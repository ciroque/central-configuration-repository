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

    val createdEnvironment = "tenv"
    val createdApplication = "tapp"

    "createdEnvironment creation" in {
      "allow adding an createdEnvironment" in {
        Put(s"$managementPath/$createdEnvironment") ~> routes ~> check {
          status must_== StatusCodes.Created
          responseAs[String] must contain(s"/${Commons.rootPath}/${Commons.managementSegment}/$createdEnvironment")

        }
      }
      "fail adding an createdEnvironment" in {
        Put(s"$managementPath/${MockSettingsDataStore.failToken}") ~> routes ~> check {
          status must_== StatusCodes.UnprocessableEntity
        }
      }
    }

    "application creation" in {
      "allow adding an application" in {
        Put(s"$managementPath/$createdEnvironment/$createdApplication") ~> routes ~> check {
          status must_== StatusCodes.Created
          responseAs[String] must contain(s"/${Commons.rootPath}/${Commons.managementSegment}/$createdEnvironment")

        }
      }
      "fail adding an application due to environment" in {
        Put(s"$managementPath/${MockSettingsDataStore.failToken}") ~> routes ~> check {
          status must_== StatusCodes.UnprocessableEntity
        }
      }
      "fail adding an application due to application" in {
        Put(s"$managementPath/$createdEnvironment/${MockSettingsDataStore.failToken}") ~> routes ~> check {
          status must_== StatusCodes.UnprocessableEntity
        }
      }
    }
  }
}
