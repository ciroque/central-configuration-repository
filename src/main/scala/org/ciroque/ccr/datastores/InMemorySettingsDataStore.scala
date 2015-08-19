package org.ciroque.ccr.datastores

import org.ciroque.ccr.datastores.DataStoreResults.{DataStoreResult, Found, NotFound}
import org.ciroque.ccr.models.ConfigurationFactory.Configuration

import scala.concurrent.Future

class InMemorySettingsDataStore extends SettingsDataStore {

  private var configurations = List[Configuration]()

  override def upsertConfiguration(configuration: Configuration): Future[DataStoreResult] = {
    configurations = configurations :+ configuration
    Future.successful(DataStoreResults.Added(configuration))
  }

  override def retrieveEnvironments(): Future[DataStoreResult] = {
    println(s"InMemoryDataStore::retrieveEnvironments()")
    Future.successful(
      composeInterstitialResultOptionFor(allEnvironments(), () => "")
    )
  }

  override def retrieveApplications(environment: String): Future[DataStoreResult] = {
    println(s"InMemoryDataStore::retrieveApplications($environment)")
    Future.successful(composeInterstitialResultOptionFor(applicationsIn(environment), () => s"environment '$environment' was not found"))
  }

  override def retrieveScopes(environment: String, application: String): Future[DataStoreResult] = {
    println(s"InMemoryDataStore::retrieveScopes($environment, $application)")
    Future.successful(composeInterstitialResultOptionFor(scopesIn(environment, application), () => s"environment '$environment' / application '$application' combination was not found"))
  }

  override def retrieveSettings(environment: String, application: String, scope: String): Future[DataStoreResult] = {
    println(s"InMemoryDataStore::retrieveSettings($environment, $application, $scope)")
    Future.successful(composeInterstitialResultOptionFor(settingsIn(environment, application, scope), () => s"environment '$environment' / application '$application' / scope '$scope' combination was not found"))
  }

  override def retrieveConfiguration(environment: String, application: String, scope: String, setting: String): Future[DataStoreResult] = {
    println(s"InMemoryDataStore::retrieveConfiguration($environment, $application, $scope, $setting)")
    Future.successful(NotFound(s"environment '$environment' / application '$application' / scope '$scope' / setting '$setting' combination was not found"))
  }

  private def allEnvironments() = {
    filteredMappedSorted(
      conf => true,
      conf => conf.key.environment
    )
  }

  private def applicationsIn(environment: String): List[String] = {
    filteredMappedSorted(
      conf => conf.key.environment == environment,
      conf => conf.key.application
    )
  }

  private def scopesIn(environment: String, application: String): List[String] = {
    filteredMappedSorted(
      conf => conf.key.environment == environment && conf.key.application == application,
      conf => conf.key.scope
    )
  }

  private def settingsIn(environment: String, application: String, scope: String): List[String] = {
    filteredMappedSorted(
      conf => conf.key.environment == environment && conf.key.application == application && conf.key.scope == scope,
      conf => conf.key.setting)
  }

  private def filteredMappedSorted(includedItems: (Configuration) => Boolean, mapping: (Configuration) => String): List[String] = {
    configurations
      .filter(includedItems)
      .map(mapping)
      .distinct
      .sortBy(s => s)
  }

  private def composeInterstitialResultOptionFor(list: List[String], buildNotFoundMessage: () => String): DataStoreResult = {
    list match {
      case Nil => NotFound(buildNotFoundMessage())
      case list: List[String] => Found(list)
    }
  }
}
