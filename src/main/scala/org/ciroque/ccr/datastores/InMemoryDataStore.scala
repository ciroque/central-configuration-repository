package org.ciroque.ccr.datastores

import org.ciroque.ccr.core.DataStoreResults.{NotFound, Found, DataStoreResult, Failure}
import org.ciroque.ccr.core.{DataStoreResults, SettingsDataStore}
import org.ciroque.ccr.models.ConfigurationFactory.Configuration

import scala.concurrent.Future

class InMemoryDataStore extends SettingsDataStore {

  var configurations = List[Configuration]()

  override def upsertConfiguration(configuration: Configuration): Future[DataStoreResult] = {
    configurations = configurations :+ configuration
    Future.successful(DataStoreResults.Found(List()))
  }

  override def retrieveEnvironments(): Future[DataStoreResult] = {
    println(s"InMemoryDataStore::retrieveEnvironments()")
    Future.successful(composeInterstitialResultOptionFor(configurations.map(conf => conf.key.environment)))
  }

  override def retrieveApplications(environment: String): Future[DataStoreResult] = {
    println(s"InMemoryDataStore::retrieveApplications($environment)")
    Future.successful(composeInterstitialResultOptionFor(applicationsIn(environment)))
  }
  
  override def retrieveScopes(environment: String, application: String): Future[DataStoreResult] = {
    println(s"InMemoryDataStore::retrieveScopes($environment, $application)")
    Future.successful(composeInterstitialResultOptionFor(scopesIn(environment, application)))
  }

  override def retrieveSettings(environment: String, application: String, scope: String): Future[DataStoreResult] = {
    println(s"InMemoryDataStore::retrieveSettings($environment, $application, $scope)")
    Future.successful(composeInterstitialResultOptionFor(settingsIn(environment, application, scope)))
  }

  override def retrieveConfiguration(environment: String, application: String, scope: String, setting: String): Future[DataStoreResult] = {
    println(s"InMemoryDataStore::retrieveConfiguration($environment, $application, $scope, $setting)")
    Future.successful(NotFound("configuration", setting))
  }

  private def applicationsIn(environment: String): List[String] = {
    configurations
      .filter(configuration => configuration.key.environment == environment)
      .map(configuration => configuration.key.application)
  }
  
  private def scopesIn(environment: String, application: String): List[String] = {
    configurations
    .filter(
        configuration => configuration.key.environment == environment
          && configuration.key.application == application)
    .map(configuration => configuration.key.scope)
  }

  private def settingsIn(environment: String, application: String, scope: String): List[String] = {
    configurations
    .filter(conf => conf.key.environment == environment && conf.key.application == application && conf.key.scope == scope)
    .map(conf => conf.key.setting)
  }

  private def composeInterstitialResultOptionFor(list: List[String]): DataStoreResult = {
    list match {
      case Nil => NotFound("TBD", "Unknown")
      case list: List[String] => Found(list)
    }
  }
}
