package org.ciroque.ccr.core

import org.ciroque.ccr.models.SettingFactory.Setting

trait SettingsDataStore {
  def retrieveApplications(environment: String): Option[List[String]]
  def retrieveEnvironments: Option[List[String]]
  def retrieveScopes(environment: String, application: String): Option[List[String]]
  def retrieveSettingNames(environment: String, application: String, scope: String): Option[List[String]]
  def retrieveSetting(environment: String, application: String, scope: String, setting: String): Option[Setting]
}
