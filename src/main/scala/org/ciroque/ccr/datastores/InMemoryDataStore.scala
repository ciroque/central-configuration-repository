package org.ciroque.ccr.datastores

import org.ciroque.ccr.core.DataStoreResults.{NotFound, Found, DataStoreResult, Failure}
import org.ciroque.ccr.core.{DataStoreResults, SettingsDataStore}
import org.ciroque.ccr.models.ConfigurationFactory.Configuration

import scala.concurrent.Future

class InMemoryDataStore extends SettingsDataStore {

  type ConfigurationList = List[Configuration]
  type SettingsMap = Map[String, ConfigurationList]
  type ScopesMap = Map[String, SettingsMap]
  type ApplicationsMap = Map[String, ScopesMap]
  type EnvironmentsMap = Map[String, ApplicationsMap]

  //var environments: Map[String, Map[String, Map[String, List[String]]]] = Map()
  var environments: EnvironmentsMap = Map()

  override def createEnvironment(environment: String): DataStoreResult = {
    if (!environments.contains(environment)) {
      environments += (environment -> Map())
    }
    DataStoreResults.Success()
  }

  override def createApplication(environment: String, application: String): DataStoreResult = {
    if (environments.contains(environment)) {
      var applications = environments.apply(environment)
      if (!applications.contains(application)) {
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
    if (environments.contains(environment)) {
      var applications = environments.apply(environment)
      if (applications.contains(application)) {
        var scopes = applications.apply(application)
        if (!scopes.contains(scope)) {
          scopes += (scope -> Map())
          applications += (application -> scopes)
        }
        environments += (environment -> applications)
      }
    }
    println(environments)
    DataStoreResults.Success()
  }

  override def upsertConfiguration(environment: String, application: String, scope: String, setting: String, configuration: Configuration): DataStoreResult =
  //    Failure("Not Implemented")
    DataStoreResults.Success()

  override def retrieveEnvironments(): Future[DataStoreResult] = {
    println(s"InMemoryDataStore::retrieveEnvironments($environments)")
    Future.successful(composeInterstitialResultOptionFor {
      environments.keys.toList
    })
  }

  override def retrieveApplications(environment: String): Future[DataStoreResult] = {
    println(s"InMemoryDataStore::retrieveApplications($environment)")
    Future.successful(composeInterstitialResultOptionFor(namesOf(applicationsIn(environment))))
  }
  
  override def retrieveScopes(environment: String, application: String): Future[DataStoreResult] = {
    println(s"InMemoryDataStore::retrieveScopes($environment, $application)")
    Future.successful(composeInterstitialResultOptionFor(namesOf(scopesIn(environment, application))))
  }

  override def retrieveSettings(environment: String, application: String, scope: String): Future[DataStoreResult] = {
    println(s"InMemoryDataStore::retrieveSettings($environment, $application, $scope)")
    Future.successful(composeInterstitialResultOptionFor(namesOf(settingsIn(environment, application, scope))))
  }

  override def retrieveConfiguration(environment: String, application: String, scope: String, setting: String): Future[DataStoreResult] = {
    println(s"InMemoryDataStore::retrieveConfiguration($environment, $application, $scope, $setting)")
    Future.successful(NotFound("configuration", setting))
  }

  private def applicationsIn(environment: String): ApplicationsMap = {
    environments.getOrElse(environment, Map())
  }
  
  private def scopesIn(environment: String, application: String): ScopesMap = {
    applicationsIn(environment).getOrElse(application, Map())
  }

  private def settingsIn(environment: String, application: String, scope: String): SettingsMap = {
    scopesIn(environment, application).getOrElse(scope, Map())
  }

  private def namesOf(map: Map[String, Any]): List[String] = map.keys.toList
  
  private def composeInterstitialResultOptionFor(list: List[String]): DataStoreResult = {
    list match {
      case Nil => NotFound("TBD", "Unknown")
      case list: List[String] => Found(list)
    }
  }
}
