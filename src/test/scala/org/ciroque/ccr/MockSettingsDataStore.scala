package org.ciroque.ccr

import org.ciroque.ccr.core.DataStoreResults.{Success, Failure, DataStoreResult}
import org.ciroque.ccr.core.SettingsDataStore
import org.ciroque.ccr.models.SettingFactory
import org.ciroque.ccr.models.SettingFactory.Setting
import org.joda.time.{DateTime, DateTimeZone}

object MockSettingsDataStore {
  val failToken = "fails"
}

class MockSettingsDataStore extends SettingsDataStore {
  
  override def retrieveApplications(environment: String): Option[List[String]] = {
    settings.filter(setting => setting.key.environment == environment).map(setting => setting.key.application).distinct match {
      case List() => None
      case _ => Some(settings.filter(setting => setting.key.environment == environment).map(setting => setting.key.application).distinct)
    }
  }

  override def retrieveEnvironments: Option[List[String]] =
    Some(settings.map(setting => setting.key.environment).distinct :+ "global")

  override def retrieveScopes(environment: String, application: String): Option[List[String]] = {
    settings.filter(setting => setting.key.environment == environment && setting.key.application == application).map(setting => setting.key.scope).distinct match {
      case List() => None
      case _ => Some(settings.filter(setting => setting.key.environment == environment && setting.key.application == application).map(setting => setting.key.scope).distinct)
    }
  }

  override def retrieveSettingNames(environment: String, application: String, scope: String): Option[List[String]] = {
    settings.filter(setting => setting.key.environment == environment && setting.key.application == application && setting.key.scope == scope).map(setting => setting.key.setting).distinct match {
      case List() => None
      case _ => Some(settings.filter(setting => setting.key.environment == environment && setting.key.application == application && setting.key.scope == scope).map(setting => setting.key.setting).distinct)
    }
  }

  override def retrieveSetting(environment: String, application: String, scope: String, settingName: String): Option[Setting] = {
    settings.filter(setting => setting.key.environment == environment && setting.key.application == application && setting.key.scope == scope && setting.key.setting == settingName).map(setting => setting) match {
      case List() => None
      case head::tail => Some(head)
    }
  }

  val effectiveAt = DateTime.now(DateTimeZone.UTC).withZone(DateTimeZone.forID("America/New_York"))
  val expiresAt = effectiveAt.plusMonths(6)
  val primarySetting = SettingFactory("dev", "ui", "logging", "log-level", "INFO", effectiveAt, expiresAt, 5000)
  
  val settings: List[Setting] = List(
    primarySetting,
    SettingFactory("qa", "ui", "logging", "log-level", "DEBUG", effectiveAt, expiresAt, 5000),
    SettingFactory("prod", "ui", "logging", "log-level", "WARN", effectiveAt, expiresAt, 5000),
    SettingFactory("dev", "ui", "logging", "logfile", "/var/log/ui.log", effectiveAt, expiresAt, 5000),
    SettingFactory("dev", "svc", "global", "svc-endpoint", "/my-service", effectiveAt, expiresAt, 5000),
    SettingFactory("dev", "svc", "logging", "log-level", "ALL", effectiveAt, expiresAt, 5000),
    SettingFactory("qa", "svc", "logging", "log-level", "INFO", effectiveAt, expiresAt, 5000),
    SettingFactory("prod", "svc", "logging", "log-level", "INFO", effectiveAt, expiresAt, 5000),
    SettingFactory("prod", "svc", "global", "app-skin", "camo1", effectiveAt, expiresAt, 5000),
    SettingFactory("prod", "svc", "global", "timeout", "1000", effectiveAt, expiresAt, 5000)
  )

  val invalidEnvironmentError = "Invalid Environment"
  val invalidApplicationError = "Invalid Application"
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
}
