package org.ciroque.ccr.datastores

import java.util.UUID

import org.ciroque.ccr.core.Commons
import org.ciroque.ccr.datastores.DataStoreResults.{DataStoreResult, Deleted, Found, NotFound}
import org.ciroque.ccr.logging.ImplicitLogging._
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.models.ConfigurationFactory.Configuration
import org.slf4j.Logger

import scala.concurrent.Future

class InMemorySettingsDataStore(implicit val logger: Logger) extends SettingsDataStore {
  def reset(): Unit = configurations = List[Configuration]()

  private var configurations = List[Configuration]()

  override def insertConfiguration(configuration: Configuration): Future[DataStoreResult] = {
    withImplicitLogging("InMemorySettingsDataStore::insertConfiguration") {
      val validatedConfiguration = configuration.copy(key = validateKey(configuration.key))
      recordValue("given-configuration", configuration.toJson.toString())
      recordValue("added-configuration", validatedConfiguration.toJson.toString())
      if(idAlreadyInCollection(validatedConfiguration._id)) {
        Future.successful(DataStoreResults.Errored(validatedConfiguration, Commons.DatastoreErrorMessages.DuplicateKeyError))
      } else {
        configurations = configurations :+ validatedConfiguration
        Future.successful(DataStoreResults.Added(validatedConfiguration))
      }
    }
  }

  override def updateConfiguration(configuration: Configuration): Future[DataStoreResult] = {
    withImplicitLogging("InMemorySettingsDataStore::updateConfiguration") {
      val existing = findById(configuration._id)
      val validatedConfiguration = configuration.copy(key = validateKey(configuration.key))
      recordValue("original-configuration", configuration.toJson.toString())
      recordValue("validated-configuration", validatedConfiguration.toJson.toString())
      if (existing.isDefined) {
        val actualExisting = existing.get
        configurations = configurations.updated(configurations.indexOf(actualExisting), validatedConfiguration)
        Future.successful(DataStoreResults.Updated(actualExisting, configuration))
      } else {
        Future.successful(DataStoreResults.NotFound(Some(validatedConfiguration), Commons.DatastoreErrorMessages.NotFoundError))
      }
    }
  }

  override def retrieveEnvironments(): Future[DataStoreResult] = {
    withImplicitLogging("InMemorySettingsDataStore.retrieveEnvironments") {
      Future.successful(composeInterstitialResultOptionFor(allEnvironments(), () => ""))
    }
  }

  private def allEnvironments() = {
    filteredMappedSorted(
      conf => true,
      conf => conf.key.environment
    )
  }

  override def retrieveApplications(environment: String): Future[DataStoreResult] = {
    withImplicitLogging("InMemorySettingsDataStore.retrieveApplications") {
      recordValue(Commons.KeyStrings.EnvironmentKey, environment)
      Future.successful(composeInterstitialResultOptionFor(applicationsIn(environment), () => s"environment '$environment' was not found"))
    }
  }

  private def applicationsIn(environment: String): List[String] = {
    val regex = checkWildcards(environment)
    filteredMappedSorted(
      conf => regex.findFirstIn(conf.key.environment).isDefined,
      conf => conf.key.application
    )
  }

  override def retrieveScopes(environment: String, application: String): Future[DataStoreResult] = {
    withImplicitLogging("InMemorySettingsDataStore.retrieveScopes") {
      recordValue(Commons.KeyStrings.EnvironmentKey, environment)
      recordValue(Commons.KeyStrings.ApplicationKey, application)
      Future.successful(composeInterstitialResultOptionFor(scopesIn(environment, application), () => s"environment '$environment' / application '$application' combination was not found"))
    }
  }

  private def scopesIn(environment: String, application: String): List[String] = {
    val environmentRegex = checkWildcards(environment)
    val applicationRegex = checkWildcards(application)
    filteredMappedSorted(
      conf => environmentRegex.findFirstIn(conf.key.environment).isDefined
        && applicationRegex.findFirstIn(conf.key.application).isDefined,
      conf => conf.key.scope
    )
  }

  override def retrieveSettings(environment: String, application: String, scope: String): Future[DataStoreResult] = {
    withImplicitLogging("InMemorySettingsDataStore.retrieveSettings") {
      recordValue(Commons.KeyStrings.EnvironmentKey, environment)
      recordValue(Commons.KeyStrings.ApplicationKey, application)
      recordValue(Commons.KeyStrings.ScopeKey, scope)
      Future.successful(composeInterstitialResultOptionFor(settingsIn(environment, application, scope), () => s"environment '$environment' / application '$application' / scope '$scope' combination was not found"))
    }
  }

  private def findById(id: UUID): Option[Configuration] = {
    applyFilter(c => c._id == id).headOption
  }

  private def idAlreadyInCollection(id: UUID): Boolean = {
    findById(id).isDefined
  }

  private def settingsIn(environment: String, application: String, scope: String): List[String] = {
    val environmentRegex = checkWildcards(environment)
    val applicationRegex = checkWildcards(application)
    val scopeRegex = checkWildcards(scope)
    filteredMappedSorted(
      conf => environmentRegex.findFirstIn(conf.key.environment).isDefined
        && applicationRegex.findFirstIn(conf.key.application).isDefined
        && scopeRegex.findFirstIn(conf.key.scope).isDefined,
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
      case Nil => NotFound(None, buildNotFoundMessage())
      case list: List[String] => Found(list)
    }
  }

  private def getConfigurations(environment: String, application: String, scope: String, setting: String): List[Configuration] ={
    val environmentRegex = checkWildcards(environment)
    val applicationRegex = checkWildcards(application)
    val scopeRegex = checkWildcards(scope)
    val settingRegex = checkWildcards(setting)

    val configs = applyFilter(
      conf =>
        (environmentRegex.findFirstIn(conf.key.environment).isDefined || conf.key.environment == ConfigurationFactory.DefaultEnvironment)
          && applicationRegex.findFirstIn(conf.key.application).isDefined
          && scopeRegex.findFirstIn(conf.key.scope).isDefined
          && settingRegex.findFirstIn(conf.key.setting).isDefined
    )
    configs
  }

  override def retrieveConfiguration(environment: String, application: String, scope: String, setting: String, sourceId: Option[String] = None): Future[DataStoreResult] = {
    withImplicitLogging("InMemorySettingsDataStore.retrieveConfiguration") {
      import org.ciroque.ccr.core.Commons
      recordValue(Commons.KeyStrings.EnvironmentKey, environment)
      recordValue(Commons.KeyStrings.ApplicationKey, application)
      recordValue(Commons.KeyStrings.ScopeKey, scope)
      recordValue(Commons.KeyStrings.SettingKey, setting)

      val configs = getConfigurations(environment, application, scope, setting)

      def findActives = configs.filter(_.isActive)

      val result = configs match {
        case Nil => NotFound(None, s"${Commons.KeyStrings.EnvironmentKey} '$environment' / ${Commons.KeyStrings.ApplicationKey} '$application' / ${Commons.KeyStrings.ScopeKey} '$scope' / ${Commons.KeyStrings.SettingKey} '$setting' combination was not found")
        case _ => findActives match {
          case Nil => NotFound(None, s"${Commons.KeyStrings.EnvironmentKey} '$environment' / ${Commons.KeyStrings.ApplicationKey} '$application' / ${Commons.KeyStrings.ScopeKey} '$scope' / ${Commons.KeyStrings.SettingKey} '$setting' found no active configuration")
          case found: Seq[Configuration] => Found(filterBySourceId(found, sourceId))
        }
      }

      Future.successful(result)
    }
  }

  override def deleteConfiguration(configuration: Configuration): Future[DataStoreResult] = Future.successful(Deleted(configuration))

  override def retrieveConfigurationSchedule(environment: String, application: String, scope: String, setting: String): Future[DataStoreResult] = {
    import org.ciroque.ccr.core.Commons.Joda._
    val dataStoreResult = getConfigurations(environment, application, scope, setting) match {
      case Nil => NotFound(None, s"${Commons.KeyStrings.EnvironmentKey} '$environment' / ${Commons.KeyStrings.ApplicationKey} '$application' / ${Commons.KeyStrings.ScopeKey} '$scope' / ${Commons.KeyStrings.SettingKey} '$setting' combination was not found")
      case list => Found(list.sortBy(c => c.temporality.effectiveAt))
    }

    Future.successful(dataStoreResult)
  }
}
