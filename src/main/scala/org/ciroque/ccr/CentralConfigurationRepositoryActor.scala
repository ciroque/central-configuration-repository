package org.ciroque.ccr

import akka.actor.ActorRefFactory
import org.ciroque.ccr.datastores.SettingsDataStore
import spray.routing.HttpServiceActor

class CentralConfigurationRepositoryActor(ds: SettingsDataStore)
  extends HttpServiceActor {

  override def actorRefFactory = context

  val configurationProviderService = new ConfigurationProviderService {
    override implicit def actorRefFactory: ActorRefFactory = context
    override implicit val dataStore: SettingsDataStore = ds
  }

  val configurationManagementService = new ConfigurationManagementService {
    override implicit def actorRefFactory: ActorRefFactory = context
    override implicit val dataStore: SettingsDataStore = ds
  }

  override def receive: Receive = runRoute(configurationManagementService.routes ~ configurationProviderService.routes)
}
