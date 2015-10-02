package org.ciroque.ccr.datastores

import org.ciroque.ccr.datastores.DataStoreResults.{DataStoreResult, Found, NotFound}
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.models.ConfigurationFactory.Configuration
import org.ciroque.ccr.logging.ImplicitLogging._
import org.slf4j.Logger

import scala.concurrent.Future
import org.ciroque.ccr.core.Commons

class InMemorySettingsDataStore(implicit val logger: Logger) extends SettingsDataStore {

  private var configurations = List[Configuration]()

  override def upsertConfiguration(configuration: Configuration): Future[DataStoreResult] = {
    withImplicitLogging("InMemorySettingsDataStore::upsertConfiguration") {
      recordValue("added-configuration", configuration.toJson.toString())
      configurations = configurations :+ configuration
      Future.successful(DataStoreResults.Added(configuration))
    }
  }

  override def retrieveEnvironments(): Future[DataStoreResult] = {
    withImplicitLogging("InMemorySettingsDataStore.retrieveEnvironments") {
      Future.successful(composeInterstitialResultOptionFor(allEnvironments(), () => ""))
    }
  }

  override def retrieveApplications(environment: String): Future[DataStoreResult] = {
    withImplicitLogging("InMemorySettingsDataStore.retrieveApplications") {
      recordValue(Commons.KeyStrings.EnvironmentKey, environment)
      Future.successful(composeInterstitialResultOptionFor(applicationsIn(environment), () => s"environment '$environment' was not found"))
    }
  }

  override def retrieveScopes(environment: String, application: String): Future[DataStoreResult] = {
    withImplicitLogging("InMemorySettingsDataStore.retrieveScopes") {
      recordValue(Commons.KeyStrings.EnvironmentKey, environment)
      recordValue(Commons.KeyStrings.ApplicationKey, application)
      Future.successful(composeInterstitialResultOptionFor(scopesIn(environment, application), () => s"environment '$environment' / application '$application' combination was not found"))
    }
  }

  override def retrieveSettings(environment: String, application: String, scope: String): Future[DataStoreResult] = {
    withImplicitLogging("InMemorySettingsDataStore.retrieveSettings") {
      recordValue(Commons.KeyStrings.EnvironmentKey, environment)
      recordValue(Commons.KeyStrings.ApplicationKey, application)
      recordValue(Commons.KeyStrings.ScopeKey, scope)
      Future.successful(composeInterstitialResultOptionFor(settingsIn(environment, application, scope), () => s"environment '$environment' / application '$application' / scope '$scope' combination was not found"))
    }
  }

  override def retrieveConfiguration(environment: String, application: String, scope: String, setting: String): Future[DataStoreResult] = {
    withImplicitLogging("InMemorySettingsDataStore.retrieveConfiguration") {
      import org.ciroque.ccr.core.Commons
      recordValue(Commons.KeyStrings.EnvironmentKey, environment)
      recordValue(Commons.KeyStrings.ApplicationKey, application)
      recordValue(Commons.KeyStrings.ScopeKey, scope)
      recordValue(Commons.KeyStrings.SettingKey, setting)
      val configs = applyFilter(
        conf =>
          (conf.key.environment == environment || conf.key.environment == ConfigurationFactory.DefaultEnvironment)
            && conf.key.application == application
            && conf.key.scope == scope
            && conf.key.setting == setting
      )

      def findActives = configs.filter(_.isActive)

      val result = configs match {
        case Nil => NotFound(s"${Commons.KeyStrings.EnvironmentKey} '$environment' / ${Commons.KeyStrings.ApplicationKey} '$application' / ${Commons.KeyStrings.ScopeKey} '$scope' / ${Commons.KeyStrings.SettingKey} '$setting' combination was not found")
        case _ => findActives match {
          case Nil => NotFound(s"${Commons.KeyStrings.EnvironmentKey} '$environment' / ${Commons.KeyStrings.ApplicationKey} '$application' / ${Commons.KeyStrings.ScopeKey} '$scope' / ${Commons.KeyStrings.SettingKey} '$setting' found no active configuration")
          case found: Seq[Configuration] => Found(found)
        }
      }

      Future.successful(result)
    }
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

  private def filteredMappedSorted(include: (Configuration) => Boolean, mapping: (Configuration) => String): List[String] = {
    applyFilter(include)
      .map(mapping)
      .distinct
      .sortBy(s => s)
  }

  private def applyFilter(include: (Configuration) => Boolean): List[Configuration] = {
    configurations.filter(include)
  }

  private def composeInterstitialResultOptionFor(list: List[String], buildNotFoundMessage: () => String): DataStoreResult = {
    list match {
      case Nil => NotFound(buildNotFoundMessage())
      case list: List[String] => Found(list)
    }
  }
}
