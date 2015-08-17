package org.ciroque.ccr.core

import org.ciroque.ccr.core.DataStoreResults.DataStoreResult
import org.ciroque.ccr.models.ConfigurationFactory.Configuration

import scala.concurrent.Future

object DataStoreResults {
  trait DataStoreResult

  case class Added[T](item: T) extends DataStoreResult
  case class Found[T](items: List[T]) extends DataStoreResult
  case class NotFound(key: String, value: String) extends DataStoreResult
  case class NoChildrenFound(key: String, value: String) extends DataStoreResult
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
