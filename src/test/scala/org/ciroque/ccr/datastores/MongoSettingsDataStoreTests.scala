package org.ciroque.ccr.datastores

import com.mongodb.casbah.MongoClient
import org.scalatest.BeforeAndAfterAll

class MongoSettingsDataStoreTests
  extends SettingsDataStoreTests
  with BeforeAndAfterAll {

  val settings: DataStoreProperties = new DataStoreProperties("localhost", 27017, "test-ccr", "configurations", "", "")

  override implicit val settingsDataStore: SettingsDataStore = new MongoSettingsDataStore(settings)

  override def afterAll() = {
    MongoClient(settings.hostname, settings.port)(settings.databaseName)(settings.catalog).drop()
  }
}
