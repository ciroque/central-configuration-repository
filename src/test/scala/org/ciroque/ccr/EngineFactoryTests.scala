package org.ciroque.ccr

import com.typesafe.config.ConfigFactory
import org.ciroque.ccr.datastores.{MongoSettingsDataStore, InMemorySettingsDataStore}
import org.scalatest.{FunSpec, Matchers}

class EngineFactoryTests extends FunSpec with Matchers {
  describe("EngineFactory") {
    describe("InMemorySettingsDataStorage configurations") {
      it("should create an InMemorySettingsDataStore given the corresponding configuration") {
        val cfg = ConfigFactory.load("InMemorySettingsDataStore-valid.conf")
        val dataStoreConfig = cfg.getConfig("ccr.engines")

        val dataStore = EngineFactory.buildStorageInstance(dataStoreConfig)

        dataStore shouldBe a[InMemorySettingsDataStore]
      }

      it("should return an InMemorySettingsDataStore given a config with no datastore defined") {
        val cfg = ConfigFactory.load("NoDataStoreEntries.conf")
        val dataStoreConfig = cfg.getConfig("ccr.engines")

        val dataStore = EngineFactory.buildStorageInstance(dataStoreConfig)

        dataStore shouldBe a[InMemorySettingsDataStore]
      }
    }
    describe("MongoSettingsDataStore configurations") {
      it("should create a MongoSettingsDataStore configured correctly with a valid configuration") {
        val cfg = ConfigFactory.load("MongoSettingsDataStore-valid.conf")
        val dataStoreConfig = cfg.getConfig("ccr.engines")

        val dataStore = EngineFactory.buildStorageInstance(dataStoreConfig)

        dataStore shouldBe a[MongoSettingsDataStore]
      }
    }
  }

}
