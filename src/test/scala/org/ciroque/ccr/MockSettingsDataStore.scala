package org.ciroque.ccr

import org.ciroque.ccr.core.SettingsDataStore

class MockSettingsDataStore extends SettingsDataStore {
  override def retrieveEnvironments: Option[List[String]] = Some(List("dev", "qa", "beta", "staging", "prod"))

  override def retrieveApplications(environment: String): Option[List[String]] = {
    val map = Map("dev" -> List("dev-app-one", "dev-app-two"))
    map.get(environment)
  }
}
