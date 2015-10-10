package org.ciroque.ccr.datastores

import org.ciroque.ccr.datastores.DataStoreResults.DataStoreResult
import org.ciroque.ccr.models.ConfigurationFactory.{Configuration, Key}
import org.slf4j.Logger

import scala.concurrent.Future

abstract class SettingsDataStore(implicit private val logger: Logger) extends CcrTypes {

  final val SOURCE_ID_MAX_LENGTH = 64

  def upsertConfiguration(configuration: Configuration): Future[DataStoreResult]

  def retrieveApplications(environment: String): Future[DataStoreResult]

  def retrieveEnvironments(): Future[DataStoreResult]

  def retrieveScopes(environment: String, application: String): Future[DataStoreResult]

  def retrieveSettings(environment: String, application: String, scope: String): Future[DataStoreResult]

  def retrieveConfiguration(environment: String, application: String, scope: String, setting: String, sourceId: Option[String] = None): Future[DataStoreResult]

  protected def checkWildcards(input: String) = {
    if (input.indexOf(".*") > -1)
      input.r
    else if (input.indexOf("*") > -1)
      input.replace("*", ".*").r
    else
      input.r
  }

  protected def filterBySourceId(list: List[Configuration], sourceId: Option[String]): List[Configuration] = {
    sourceId match {
      case None => list
      case Some(_) => list.filter(c => c.key.sourceId == sourceId) match {
        case Nil => list
        case filtered => filtered
      }
    }
  }

  protected def validateKey(key: Key): Key = {
    key.sourceId match {
      case Some(sourceIdValue) if sourceIdValue.length > SOURCE_ID_MAX_LENGTH =>
        key.copy(sourceId = Some(sourceIdValue.substring(0, SOURCE_ID_MAX_LENGTH)))
      case _ => key
    }
  }
}
