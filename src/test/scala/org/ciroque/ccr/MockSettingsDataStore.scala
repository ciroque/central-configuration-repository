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

  override def retrieveEnvironments: Option[List[String]] = Some(List("dev", "qa", "beta", "staging", "prod"))

  override def retrieveScopes(environment: String, application: String): Option[List[String]] = Some(List("logging", "global"))

  override def retrieveSettingNames(environment: String, application: String, scope: String): Option[List[String]] = Some(List("user.timeout", "user.appskin"))

  override def retrieveSetting(environment: String, application: String, scope: String, seatting: String): Option[Setting] = {
    Some(setting)
  }

  val effectiveAt = DateTime.now(DateTimeZone.UTC).withZone(DateTimeZone.forID("America/New_York"))
  val expiresAt = effectiveAt.plusMonths(6)
  val setting = SettingFactory("env", "app", "scope", "setting", "1000000", effectiveAt, expiresAt, 5000)
}
