package org.ciroque.ccr.datastores

import org.ciroque.ccr.core.DataStoreResults.{DataStoreResult, Failure}
import org.ciroque.ccr.core.{DataStoreResults, SettingsDataStore}
import org.ciroque.ccr.models.ConfigurationFactory.Configuration

class InMemoryDataStore extends SettingsDataStore {
  var environments: List[String] = List()

  override def createEnvironment(environment: String): DataStoreResult =  {
    if(!environments.contains(environment)) {
      environments = environments :+ environment
    }
    DataStoreResults.Success()
  }
  override def createApplication(environment: String, application: String): DataStoreResult = Failure("Not Implemented")
  override def createScope(environment: String, application: String, scope: String): DataStoreResult = Failure("Not Implemented")
  override def upsertConfiguration(environment: String, application: String, scope: String, setting: String, configuration: Configuration): DataStoreResult = Failure("Not Implemented")

  override def retrieveEnvironments(): InterstitialResultOption = {
    println(s"InMemoryDataStore::retrieveEnvironments($environments)")
    Some(environments)
  }
  override def retrieveApplications(environment: String): InterstitialResultOption = {
    println(s"InMemoryDataStore::retrieveApplications($environment)")
    Some(List())
  }
  override def retrieveScopes(environment: String, application: String): InterstitialResultOption = {
    println(s"InMemoryDataStore::retrieveScopes($environment, $application)")
    Some(List())
  }
  override def retrieveSettings(environment: String, application: String, scope: String): InterstitialResultOption = {
    println(s"InMemoryDataStore::retrieveSettings($environment, $application, $scope)")
    Some(List())
  }
  override def retrieveConfiguration(environment: String, application: String, scope: String, setting: String): Option[Configuration] = {
    println(s"InMemoryDataStore::retrieveConfiguration($environment, $application, $scope, $setting)")
    None
  }
}
