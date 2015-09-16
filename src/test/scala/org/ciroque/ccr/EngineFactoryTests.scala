package org.ciroque.ccr

import com.typesafe.config.ConfigFactory
import org.ciroque.ccr.datastores.{SettingsDataStore, MongoSettingsDataStore, InMemorySettingsDataStore}
import org.scalatest.{FunSpec, Matchers}

class EngineFactoryTests extends FunSpec with Matchers {
  describe("EngineFactory") {
    describe("InMemorySettingsDataStorage configurations") {
      it("should create an InMemorySettingsDataStore given the corresponding configuration") {
        val dataStore = buildStorageInstance("InMemorySettingsDataStore-valid.conf")
        dataStore shouldBe a[InMemorySettingsDataStore]
      }

      it("should return an InMemorySettingsDataStore given a config with no datastore defined") {
        val dataStore = buildStorageInstance("NoDataStoreEntries.conf")
        dataStore shouldBe a[InMemorySettingsDataStore]
      }
    }

    describe("MongoSettingsDataStore configurations") {
      it("should create a MongoSettingsDataStore configured correctly with a valid configuration") {
        val dataStore = buildStorageInstance("MongoSettingsDataStore-valid.conf")
        dataStore shouldBe a[MongoSettingsDataStore]
      }
    }
  }

  private def buildStorageInstance(filename: String): SettingsDataStore = {
    val config = ConfigFactory.load(filename)
    EngineFactory.buildStorageInstance(config)
  }
}
