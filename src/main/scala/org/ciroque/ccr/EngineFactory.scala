package org.ciroque.ccr

import org.ciroque.ccr.core.Commons
import org.ciroque.ccr.datastores._
import org.slf4j.LoggerFactory

object EngineFactory {
  def buildStorageInstance(dataStoreConfig: DataStoreConfig): SettingsDataStore = {
    val logger = LoggerFactory.getLogger(Commons.KeyStrings.actorSystemName)

    dataStoreConfig.clazz match {
      case None | Some("InMemoryDataStore") => new InMemorySettingsDataStore()(logger)
      case Some("MongoSettingsDataStore") =>
        new MongoSettingsDataStore(dataStoreConfig.params)(logger)
      case _ => throw new Exception(s"Unknown SettingsDataStore class:  ${dataStoreConfig.clazz}")
    }
  }
}
