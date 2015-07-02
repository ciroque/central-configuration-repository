package org.ciroque.ccr

import org.ciroque.ccr.core.SettingsDataStore
import org.ciroque.ccr.models.SettingFactory
import org.ciroque.ccr.models.SettingFactory.Setting
import org.joda.time.{DateTime, DateTimeZone}

class MockSettingsDataStore extends SettingsDataStore {
  
  override def retrieveApplications(environment: String): Option[List[String]] = {
    val map = Map("dev" -> List("dev-app-one", "dev-app-two"))
    map.get(environment)
  }

  override def retrieveEnvironments: Option[List[String]] = Some(settings.map(setting => setting.key.environment).distinct)

  override def retrieveScopes(environment: String, application: String): Option[List[String]] = Some(settings.filter(setting => setting.key.environment == environment && setting.key.application == application).map(setting => setting.key.scope).distinct)

  override def retrieveSettingNames(environment: String, application: String, scope: String): Option[List[String]] =
    Some(settings
      .filter(setting => setting.key.environment == environment && setting.key.application == application && setting.key.scope == scope)
      .map(setting => setting.key.setting)
  )

  override def retrieveSetting(environment: String, application: String, scope: String, settingName: String): Option[Setting] = {
    Some(settings
      .filter(setting => setting.key.environment == environment && setting.key.application == application && setting.key.scope == scope && setting.key.setting == settingName)
      .map(setting => setting)
    .head)
//    Some(primarySetting)
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
}
