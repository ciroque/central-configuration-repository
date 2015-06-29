package org.ciroque.ccr

import akka.actor.ActorRefFactory
import spray.routing.HttpServiceActor

class CentralConfigurationRepositoryActor extends HttpServiceActor {

  val configurationProviderService = new ConfigurationProviderService {
    override implicit def actorRefFactory: ActorRefFactory = context
  }

  override def receive: Receive = runRoute(configurationProviderService.routes)
}
