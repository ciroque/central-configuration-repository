package org.ciroque.ccr

import com.typesafe.config.Config
import org.ciroque.ccr.core.Commons
import org.ciroque.ccr.datastores.{InMemorySettingsDataStore, SettingsDataStore}
import org.slf4j.LoggerFactory

object EngineFactory {

  val expectedDataStorageClassPath = "datastore.class"

  def buildStorageInstance(dataStoreConfig: Config): SettingsDataStore = {
    val logger = LoggerFactory.getLogger(Commons.KeyStrings.actorSystemName)

    val clazz = getConfigStringAt(dataStoreConfig, expectedDataStorageClassPath)
    clazz match {
      case None | Some("InMemoryDataStore") => new InMemorySettingsDataStore()(logger)
      case _ => throw new Exception(s"Unknown SettingsDataStore class:  $clazz")
    }
  }

  private def getConfigStringAt(config: Config, path: String): Option[String] = {
    config.hasPath(path) match {
      case true => Some(config.getString(path))
      case false => None
    }
  }
}
