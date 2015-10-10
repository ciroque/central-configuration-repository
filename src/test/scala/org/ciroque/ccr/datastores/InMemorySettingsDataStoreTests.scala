package org.ciroque.ccr.datastores

class InMemorySettingsDataStoreTests extends SettingsDataStoreTests {

  import org.ciroque.ccr.logging.CachingLogger
  import org.slf4j.Logger

  override implicit val logger: Logger = new CachingLogger()

  implicit val settingsDataStore = new InMemorySettingsDataStore()(logger)
}
