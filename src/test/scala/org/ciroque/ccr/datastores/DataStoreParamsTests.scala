package org.ciroque.ccr.datastores

import org.ciroque.ccr.core.ConfigFactory
import org.scalatest.{FunSpec, Matchers}

class DataStoreParamsTests
  extends FunSpec
  with Matchers {

  describe("DataStoreParams") {
    val datastoreParamsPath: String = "ccr.engines.dataStore.params"

    it("successfully parses a properly formatted config file") {
      val config = ConfigFactory.getPrimaryDataStoreConfig("MongoSettingsDataStore-valid.conf")

      config.clazz shouldEqual Some("MongoSettingsDataStore")
      config.params.hostname shouldEqual "localhost"
      config.params.port shouldEqual Some(MongoSettingsDataStore.defaultPort)
      config.params.catalog shouldEqual "settings"
      config.params.database shouldEqual "ccr-testing"
    }

    it("applies default hostname and database names when not present in configuration") {
      val config = ConfigFactory.getPrimaryDataStoreConfig("SettingsDataStore-MissingHostnamePortAndDatabase.conf")

      config.params.hostname shouldEqual "localhost"
      config.params.port shouldEqual None
      config.params.database shouldEqual "ccr"
      config.params.catalog shouldEqual "settings"
    }

    it("applies default values when the config is empty") {
      val config = ConfigFactory.getPrimaryDataStoreConfig("NoDataStoreEntries.conf")

      config.params.hostname shouldEqual "localhost"
      config.params.port shouldEqual None
      config.params.database shouldEqual "ccr"
      config.params.catalog shouldEqual "settings"
    }
  }
}
