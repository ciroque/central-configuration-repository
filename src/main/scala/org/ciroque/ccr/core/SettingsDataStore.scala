package org.ciroque.ccr.core

trait SettingsDataStore {
  def retrieveApplications(environment: String): Option[List[String]]
  def retrieveEnvironments: Option[List[String]]
  def retrieveScopes(environment: String, application: String): Option[List[String]]
}
