package org.ciroque.ccr

import com.typesafe.config.ConfigFactory
import org.ciroque.ccr.datastores.InMemorySettingsDataStore
import org.scalatest.{FunSpec, Matchers}

class EngineFactoryTests extends FunSpec with Matchers {
  describe("EngineFactory") {
     it("should create an InMemorySettingsDataStore given the corresponding Config") {
      val cfg = ConfigFactory.load("InMemorySettingsDataStore-test.conf")
      val dataStoreConfig = cfg.getConfig("ccr.engines")

      val dataStore = EngineFactory.buildStorageInstance(dataStoreConfig)

      dataStore shouldBe a [InMemorySettingsDataStore]
    }
  }
}
