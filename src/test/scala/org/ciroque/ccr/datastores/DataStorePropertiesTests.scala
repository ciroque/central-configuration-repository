package org.ciroque.ccr.datastores

import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, FunSpec}

class DataStorePropertiesTests
  extends FunSpec
  with Matchers {

  describe("DataSettingsProperties") {
    val datastoreParamsPath: String = "ccr.engines.datastore.params"

    it("successfully parses a properly formatted config file") {
      val config = ConfigFactory.load("MongoSettingsDataStore-valid.conf").getConfig(datastoreParamsPath)
      val dsp = DataStoreProperties.fromConfig(config)

      dsp.hostname shouldEqual "localhost"
      dsp.port shouldEqual 25026
      dsp.catalog shouldEqual "settings"
      dsp.database shouldEqual "ccr-testing"
    }

    it("applies default hostname and database names when not present in configuration") {
      val config = ConfigFactory.load("SettingsDataStore-MissingHostnamePortAndDatabase.conf").getConfig(datastoreParamsPath)
      val dsp = DataStoreProperties.fromConfig(config)

      dsp.hostname shouldEqual "localhost"
      dsp.port shouldEqual 25026
      dsp.database shouldEqual "ccr"
      dsp.catalog shouldEqual "settings"
    }

    it("applies default values when the config is empty") {
      val config = ConfigFactory.load("NoDataStoreEntries.conf").getConfig(datastoreParamsPath)
      val dsp = DataStoreProperties.fromConfig(config)

      dsp.hostname shouldEqual "localhost"
      dsp.port shouldEqual 25026
      dsp.database shouldEqual "ccr"
      dsp.catalog shouldEqual "settings"
    }
  }
}
