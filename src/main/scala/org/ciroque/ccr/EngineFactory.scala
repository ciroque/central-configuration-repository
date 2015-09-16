package org.ciroque.ccr

import com.typesafe.config.Config
import org.ciroque.ccr.core.Commons
import org.ciroque.ccr.datastores.{DataStoreProperties, MongoSettingsDataStore, InMemorySettingsDataStore, SettingsDataStore}
import org.slf4j.LoggerFactory

object EngineFactory {

  val expectedDataStorageClassPath = "datastore.class"
  val expectedDataStorageParamsPath = "datastore.params"

  def buildStorageInstance(dataStoreConfig: Config): SettingsDataStore = {
    val logger = LoggerFactory.getLogger(Commons.KeyStrings.actorSystemName)

    val clazz = {
      dataStoreConfig.hasPath(expectedDataStorageClassPath) match {
        case true => Some(dataStoreConfig.getString(expectedDataStorageClassPath))
        case false => None
      }
    }

    clazz match {
      case None | Some("InMemoryDataStore") => new InMemorySettingsDataStore()(logger)
      case Some("MongoSettingsDataStore") =>
        val properties = DataStoreProperties.fromConfig(dataStoreConfig.getConfig(expectedDataStorageParamsPath))
        new MongoSettingsDataStore(properties)(logger)
      case _ => throw new Exception(s"Unknown SettingsDataStore class:  $clazz")
    }
  }
}
