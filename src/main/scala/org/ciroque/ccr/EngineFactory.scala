package org.ciroque.ccr

import com.typesafe.config.Config
import org.ciroque.ccr.core.Commons
import org.ciroque.ccr.datastores.{InMemorySettingsDataStore, SettingsDataStore}
import org.slf4j.LoggerFactory

object EngineFactory {
  def buildStorageInstance(dataStoreConfig: Config): SettingsDataStore = {
    val logger = LoggerFactory.getLogger(Commons.KeyStrings.actorSystemName)
    val clazz = dataStoreConfig.getString("datastore.class")
    clazz match {
      case "InMemoryDataStore" => new InMemorySettingsDataStore()(logger)
      case _ => throw new Exception(s"Unknown SettingsDataStore class:  $clazz")
    }
  }
}
