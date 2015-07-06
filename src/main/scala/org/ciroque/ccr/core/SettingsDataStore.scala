package org.ciroque.ccr.core

import org.ciroque.ccr.core.DataStoreResults.DataStoreResult
import org.ciroque.ccr.models.SettingFactory.Setting

trait CcrTypes {
  type InterstitialResultOption = Option[List[String]]
}

trait SettingsDataStore extends CcrTypes {
  def createApplication(environment: String, application: String): DataStoreResult
  def createEnvironment(environment: String): DataStoreResult
  def createScope(environment: String, application: String, scope: String): DataStoreResult

  def retrieveApplications(environment: String): InterstitialResultOption
  def retrieveEnvironments: InterstitialResultOption
  def retrieveScopes(environment: String, application: String): InterstitialResultOption
  def retrieveSettingNames(environment: String, application: String, scope: String): InterstitialResultOption
  def retrieveSetting(environment: String, application: String, scope: String, setting: String): Option[Setting]
}
