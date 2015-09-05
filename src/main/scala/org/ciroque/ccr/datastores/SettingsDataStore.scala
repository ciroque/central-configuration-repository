package org.ciroque.ccr.datastores

import org.ciroque.ccr.datastores.DataStoreResults.DataStoreResult
import org.ciroque.ccr.models.ConfigurationFactory.Configuration
import org.slf4j.Logger

import scala.concurrent.Future

abstract class SettingsDataStore(implicit private val logger: Logger) extends CcrTypes {

  def upsertConfiguration(configuration: Configuration): Future[DataStoreResult]

  def retrieveApplications(environment: String): Future[DataStoreResult]

  def retrieveEnvironments(): Future[DataStoreResult]

  def retrieveScopes(environment: String, application: String): Future[DataStoreResult]

  def retrieveSettings(environment: String, application: String, scope: String): Future[DataStoreResult]

  def retrieveConfiguration(environment: String, application: String, scope: String, setting: String): Future[DataStoreResult]
}
