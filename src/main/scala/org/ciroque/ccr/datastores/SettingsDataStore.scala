package org.ciroque.ccr.datastores

import org.ciroque.ccr.datastores.DataStoreResults.DataStoreResult
import org.ciroque.ccr.models.ConfigurationFactory.Configuration

import scala.concurrent.Future

object DataStoreResults {
  trait DataStoreResult

  case class Added[T](item: T) extends DataStoreResult
  case class Found[T](items: Seq[T]) extends DataStoreResult
  case class NotFound(message: String) extends DataStoreResult
  case class Failure(message: String, cause: Throwable = null) extends DataStoreResult
}

trait CcrTypes {
  type InterstitialResultOption = Option[List[String]]
}

trait SettingsDataStore extends CcrTypes {
  def upsertConfiguration(configuration: Configuration): Future[DataStoreResult]

  def retrieveApplications(environment: String): Future[DataStoreResult]
  def retrieveEnvironments(): Future[DataStoreResult]
  def retrieveScopes(environment: String, application: String): Future[DataStoreResult]
  def retrieveSettings(environment: String, application: String, scope: String): Future[DataStoreResult]
  def retrieveConfiguration(environment: String, application: String, scope: String, setting: String): Future[DataStoreResult]
}
