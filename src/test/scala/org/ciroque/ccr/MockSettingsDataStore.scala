package org.ciroque.ccr

import org.ciroque.ccr.core.DataStoreResults._
import org.ciroque.ccr.core.SettingsDataStore
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.models.ConfigurationFactory.Configuration
import org.joda.time.{DateTime, DateTimeZone}

object MockSettingsDataStore {
  val failToken = "fails"
}

@deprecated("just don't use it. switching to EasyMock")
class MockSettingsDataStore extends SettingsDataStore {

  override def retrieveApplications(environment: String): DataStoreResult = {
    settings.filter(setting => setting.key.environment == environment).map(setting => setting.key.application).distinct match {
      case Nil => NotFound(environment)
      case environments => Found[String](environments)
    }
  }

  override def retrieveEnvironments(): DataStoreResult =
    settings.map(setting => setting.key.environment).distinct match {
      case Nil => NotFound("environments")
      case environments => Found[String]("global" +: environments)
    }

  override def retrieveScopes(environment: String, application: String): DataStoreResult = {
    settings.filter(setting => setting.key.environment == environment && setting.key.application == application).map(setting => setting.key.scope).distinct match {
      case Nil => NotFound(s"$environment / $application")
      case scopes => Found[String](scopes)
    }
  }

  override def retrieveSettings(environment: String, application: String, scope: String): DataStoreResult = {
    settings.filter(setting => setting.key.environment == environment && setting.key.application == application && setting.key.scope == scope).map(setting => setting.key.setting).distinct match {
      case Nil => NotFound(s"...")
      case foundSettings => Found(foundSettings)
    }
  }

  override def retrieveConfiguration(environment: String, application: String, scope: String, settingName: String): DataStoreResult = {
    settings.filter(setting => setting.key.environment == environment && setting.key.application == application && setting.key.scope == scope && setting.key.setting == settingName).map(setting => setting) match {
      case Nil => NotFound(s"...")
      case configurations => Found[ConfigurationFactory.Configuration](configurations)
    }
  }

  val effectiveAt = DateTime.now(DateTimeZone.UTC).withZone(DateTimeZone.forID("America/New_York"))
  val expiresAt = effectiveAt.plusMonths(6)
  val primarySetting = ConfigurationFactory("dev", "ui", "logging", "log-level", "INFO", effectiveAt, expiresAt, 5000)

  val settings: List[Configuration] = List(
    primarySetting,
    ConfigurationFactory("qa", "ui", "logging", "log-level", "DEBUG", effectiveAt, expiresAt, 5000),
    ConfigurationFactory("prod", "ui", "logging", "log-level", "WARN", effectiveAt, expiresAt, 5000),
    ConfigurationFactory("dev", "ui", "logging", "logfile", "/var/log/ui.log", effectiveAt, expiresAt, 5000),
    ConfigurationFactory("dev", "svc", "global", "svc-endpoint", "/my-service", effectiveAt, expiresAt, 5000),
    ConfigurationFactory("dev", "svc", "logging", "log-level", "ALL", effectiveAt, expiresAt, 5000),
    ConfigurationFactory("qa", "svc", "logging", "log-level", "INFO", effectiveAt, expiresAt, 5000),
    ConfigurationFactory("prod", "svc", "logging", "log-level", "INFO", effectiveAt, expiresAt, 5000),
    ConfigurationFactory("prod", "svc", "global", "app-skin", "camo1", effectiveAt, expiresAt, 5000),
    ConfigurationFactory("prod", "svc", "global", "timeout", "1000", effectiveAt, expiresAt, 5000)
  )

  val invalidEnvironmentError = "Invalid Environment"
  val invalidApplicationError = "Invalid Application"
  val invalidScopeError = "Invalid Scope"
  val invalidSettingError = "Invalid Setting"
  val putErrorMessage = "Something went wrong."

  override def createEnvironment(environment: String): DataStoreResult = {
    environment match {
      case MockSettingsDataStore.failToken => Failure(putErrorMessage)
      case _ => Success()
    }
  }

  override def createApplication(environment: String, application: String): DataStoreResult = {
    environment match {
      case MockSettingsDataStore.failToken => Failure(invalidEnvironmentError)
      case _ => application match {
        case MockSettingsDataStore.failToken => Failure(putErrorMessage)
        case _ => Success()
      }
    }
  }

  override def createScope(environment: String, application: String, scope: String): DataStoreResult = {
    environment match {
      case MockSettingsDataStore.failToken => Failure(invalidEnvironmentError)
      case _ => application match {
        case MockSettingsDataStore.failToken => Failure(invalidApplicationError)
        case _ => scope match {
          case MockSettingsDataStore.failToken => Failure(putErrorMessage)
          case _ => Success()
        }
      }
    }
  }

  override def upsertConfiguration(environment: String, application: String, scope: String, setting: String, configuration: Configuration): DataStoreResult = {
    environment match {
      case MockSettingsDataStore.failToken => Failure(invalidEnvironmentError)
      case _ => application match {
        case MockSettingsDataStore.failToken => Failure(invalidApplicationError)
        case _ => scope match {
          case MockSettingsDataStore.failToken => Failure(invalidScopeError)
          case _ => setting match {
            case MockSettingsDataStore.failToken => Failure(invalidSettingError)
            case _ => configuration.value match {
              case MockSettingsDataStore.failToken => Failure(putErrorMessage)
              case _ => Success()
            }
          }
        }
      }
    }
  }
}
