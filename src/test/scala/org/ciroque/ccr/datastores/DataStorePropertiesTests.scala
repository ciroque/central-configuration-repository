package org.ciroque.ccr.datastores

import com.typesafe.config.ConfigFactory
import org.scalatest.{FunSpec, Matchers}

class DataStorePropertiesTests
  extends FunSpec
  with Matchers {

  describe("DataSettingsProperties") {
    val datastoreParamsPath: String = "ccr.engines.datastore.params"

    it("successfully parses a properly formatted config file") {
      val config = ConfigFactory.load("MongoSettingsDataStore-valid.conf")
      val dsp = DataStoreProperties.fromConfig(config)

      dsp.hostname shouldEqual "localhost"
      dsp.port shouldEqual Some(MongoSettingsDataStore.defaultPort)
      dsp.catalog shouldEqual "settings"
      dsp.database shouldEqual "ccr-testing"
    }

    it("applies default hostname and database names when not present in configuration") {
      val config = ConfigFactory.load("SettingsDataStore-MissingHostnamePortAndDatabase.conf")
      val dsp = DataStoreProperties.fromConfig(config)

      dsp.hostname shouldEqual "localhost"
      dsp.port shouldEqual None
      dsp.database shouldEqual "ccr"
      dsp.catalog shouldEqual "settings"
    }

    it("applies default values when the config is empty") {
      val config = ConfigFactory.load("NoDataStoreEntries.conf")
      val dsp = DataStoreProperties.fromConfig(config)

      dsp.hostname shouldEqual "localhost"
      dsp.port shouldEqual None
      dsp.database shouldEqual "ccr"
      dsp.catalog shouldEqual "settings"
    }
  }
}
