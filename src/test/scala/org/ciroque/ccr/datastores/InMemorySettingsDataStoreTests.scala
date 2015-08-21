package org.ciroque.ccr.datastores

class InMemorySettingsDataStoreTests extends SettingsDataStoreTests {
  implicit val settingsDataStore = new InMemorySettingsDataStore()
}
