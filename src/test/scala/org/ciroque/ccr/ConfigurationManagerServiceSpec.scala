package org.ciroque.ccr

import akka.actor.ActorRefFactory
import org.ciroque.ccr.core.Commons
import org.ciroque.ccr.models.ConfigurationFactory
import org.joda.time.{DateTimeZone, DateTime}
import org.specs2.mutable.Specification
import spray.http.StatusCodes
import spray.testkit.Specs2RouteTest
import spray.httpx.SprayJsonSupport._

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
    val createdScope = "tscope"
    val createdSettingName = "tsetting"
    val createdSetting = ConfigurationFactory(
      createdEnvironment,
      createdApplication,
      createdScope,
      createdSettingName,
      "1000",
      new DateTime(DateTimeZone.UTC),
      new DateTime(DateTimeZone.UTC),
      600L)

    "createdEnvironment creation" in {
      "allow adding an createdEnvironment" in {
        Put(s"$managementPath/$createdEnvironment") ~> routes ~> check {
          status === StatusCodes.Created
          responseAs[String] must contain(s"/${Commons.rootPath}/${Commons.managementSegment}/$createdEnvironment")

        }
      }
      "fail adding an createdEnvironment" in {
        Put(s"$managementPath/${MockSettingsDataStore.failToken}") ~> routes ~> check {
          status === StatusCodes.UnprocessableEntity
        }
      }
    }

    "application creation" in {
      "allow adding an application" in {
        Put(s"$managementPath/$createdEnvironment/$createdApplication") ~> routes ~> check {
          status === StatusCodes.Created
          responseAs[String] must contain(s"/${Commons.rootPath}/${Commons.managementSegment}/$createdEnvironment")

        }
      }
      "fail adding an application due to environment" in {
        Put(s"$managementPath/${MockSettingsDataStore.failToken}/$createdApplication") ~> routes ~> check {
          status === StatusCodes.UnprocessableEntity
        }
      }
      "fail adding an application due to application" in {
        Put(s"$managementPath/$createdEnvironment/${MockSettingsDataStore.failToken}") ~> routes ~> check {
          status === StatusCodes.UnprocessableEntity
        }
      }
    }

    "scope creation" in {
      "allow adding a scope" in {
        Put(s"$managementPath/$createdEnvironment/$createdApplication/$createdScope") ~> routes ~> check {
          status === StatusCodes.Created
          responseAs[String] must contain(s"/${Commons.rootPath}/${Commons.managementSegment}/$createdEnvironment")

        }
      }
      "fail adding a scope due to environment" in {
        Put(s"$managementPath/${MockSettingsDataStore.failToken}/$createdApplication/$createdScope") ~> routes ~> check {
          status === StatusCodes.UnprocessableEntity
        }
      }
      "fail adding a scope due to application" in {
        Put(s"$managementPath/$createdEnvironment/${MockSettingsDataStore.failToken}/$createdScope") ~> routes ~> check {
          status === StatusCodes.UnprocessableEntity
        }
      }
      "fail adding a scope due to scope" in {
        Put(s"$managementPath/$createdEnvironment/$createdApplication/${MockSettingsDataStore.failToken}") ~> routes ~> check {
          status === StatusCodes.UnprocessableEntity
        }
      }
    }

    "setting creation" in {
      "allow adding a setting" in {
        import org.ciroque.ccr.models.ConfigurationFactory._
        Put(s"$managementPath/$createdEnvironment/$createdApplication/$createdScope/$createdSettingName", createdSetting) ~> routes ~> check {
          status === StatusCodes.Created
          responseAs[String] must contain(s"/${Commons.rootPath}/${Commons.managementSegment}/$createdEnvironment")
        }
      }
      "fail adding a setting due to environment" in {
        Put(s"$managementPath/${MockSettingsDataStore.failToken}/$createdApplication/$createdScope/$createdSettingName", createdSetting) ~> routes ~> check {
          status === StatusCodes.UnprocessableEntity
          responseAs[String] must contain("Invalid Environment")
        }
      }
      "fail adding a setting due to application" in {
        Put(s"$managementPath/$createdEnvironment/${MockSettingsDataStore.failToken}/$createdScope/$createdSettingName", createdSetting) ~> routes ~> check {
          status === StatusCodes.UnprocessableEntity
          responseAs[String] must contain("Invalid Application")
        }
      }
      "fail adding a setting due to scope" in {
        Put(s"$managementPath/$createdEnvironment/$createdApplication/${MockSettingsDataStore.failToken}/$createdSettingName", createdSetting) ~> routes ~> check {
          status === StatusCodes.UnprocessableEntity
          responseAs[String] must contain("Invalid Scope")
        }
      }
      "fail adding a setting due to setting" in {
        Put(s"$managementPath/$createdEnvironment/$createdApplication/$createdScope/${MockSettingsDataStore.failToken}", createdSetting) ~> routes ~> check {
          status === StatusCodes.UnprocessableEntity
          responseAs[String] must contain("Invalid Setting")
        }
      }
    }
  }
}
