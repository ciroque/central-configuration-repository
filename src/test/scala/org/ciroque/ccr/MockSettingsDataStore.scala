package org.ciroque.ccr

import org.ciroque.ccr.core.SettingsDataStore

class MockSettingsDataStore extends SettingsDataStore {
  override def retrieveEnvironments: Option[List[String]] = Some(List("dev", "qa", "beta", "staging", "prod"))}
