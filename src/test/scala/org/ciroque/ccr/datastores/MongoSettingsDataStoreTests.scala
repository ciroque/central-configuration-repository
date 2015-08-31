package org.ciroque.ccr.datastores

import com.mongodb.casbah.MongoClient
import org.scalatest.BeforeAndAfterAll

class MongoSettingsDataStoreTests
  extends SettingsDataStoreTests
  with BeforeAndAfterAll {

  import org.ciroque.ccr.logging.CachingLogger

  val settings: DataStoreProperties = new DataStoreProperties("localhost", 27017, "test-ccr", "configurations", "", "")
  override implicit val logger = new CachingLogger()

  override implicit val settingsDataStore: SettingsDataStore = new MongoSettingsDataStore(settings)(logger)

  override def afterAll() = {
    super.afterAll()
    MongoClient(settings.hostname, settings.port)(settings.databaseName)(settings.catalog).drop()
  }
}
