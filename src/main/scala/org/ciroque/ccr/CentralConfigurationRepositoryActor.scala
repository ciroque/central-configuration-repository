package org.ciroque.ccr

import akka.actor.ActorRefFactory
import org.ciroque.ccr.core.SettingsDataStore
import spray.routing.HttpServiceActor

class CentralConfigurationRepositoryActor extends HttpServiceActor {

  val configurationProviderService = new ConfigurationProviderService {
    override implicit def actorRefFactory: ActorRefFactory = context

    override implicit val dataStore: SettingsDataStore = null
  }

  override def receive: Receive = runRoute(configurationProviderService.routes)
}
