package org.ciroque.ccr

import com.typesafe.config.ConfigFactory
import org.ciroque.ccr.datastores.InMemorySettingsDataStore
import org.scalatest.{FunSpec, Matchers}

class EngineFactoryTests extends FunSpec with Matchers {
  describe("EngineFactory") {
     it("should create an InMemorySettingsDataStore given the corresponding Config") {
      val cfg = ConfigFactory.load("InMemorySettingsDataStore-valid.conf")
      val dataStoreConfig = cfg.getConfig("ccr.engines")

      val dataStore = EngineFactory.buildStorageInstance(dataStoreConfig)

      dataStore shouldBe a [InMemorySettingsDataStore]
    }
  }

  it("should return an InMemorySettingsDataStore given a config with no datastore defined") {
    val cfg = ConfigFactory.load("NoDataStoreEntries.conf")
    val dataStoreConfig = cfg.getConfig("ccr.engines")

    val dataStore = EngineFactory.buildStorageInstance(dataStoreConfig)

    dataStore shouldBe a [InMemorySettingsDataStore]
  }
}
