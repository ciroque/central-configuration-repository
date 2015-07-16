package org.ciroque.ccr.datastores

import org.ciroque.ccr.core.DataStoreResults.{DataStoreResult, Failure}
import org.ciroque.ccr.core.{DataStoreResults, SettingsDataStore}
import org.ciroque.ccr.models.ConfigurationFactory.Configuration

class InMemoryDataStore extends SettingsDataStore {

  var environments: Map[String, Map[String, Map[String, List[String]]]] = Map()

  override def createEnvironment(environment: String): DataStoreResult =  {
    if(!environments.contains(environment)) {
      environments += (environment -> Map())
    }
    DataStoreResults.Success()
  }

  override def createApplication(environment: String, application: String): DataStoreResult = {
    if(environments.contains(environment)) {
      var applications = environments.apply(environment)
      if(!applications.contains(application)) {
        applications += (application -> Map())
        environments += (environment -> applications)
      }
    } else {
      DataStoreResults.Failure(s"Environment '$environment' was not found.")
    }

    println(environments)
    DataStoreResults.Success()
  }

  override def createScope(environment: String, application: String, scope: String): DataStoreResult = {
    if(environments.contains(environment)) {
      var applications = environments.apply(environment)
      if(applications.contains(application)) {
        var scopes = applications.apply(application)
        if(!scopes.contains(scope)) {
          scopes += (scope -> List())
          applications += (application -> scopes)
        }
        environments += (environment -> applications)
      }
    }
    println(environments)
    DataStoreResults.Success()
  }


  override def upsertConfiguration(environment: String, application: String, scope: String, setting: String, configuration: Configuration): DataStoreResult = Failure("Not Implemented")

  override def retrieveEnvironments(): InterstitialResultOption = {
    println(s"InMemoryDataStore::retrieveEnvironments($environments)")
    Some(environments.keys.toList)
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
