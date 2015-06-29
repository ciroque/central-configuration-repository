package org.ciroque.ccr.core

trait SettingsDataStore {
  def retrieveEnvironments: Option[List[String]]
  def retrieveApplications(environment: String): Option[List[String]]
}
