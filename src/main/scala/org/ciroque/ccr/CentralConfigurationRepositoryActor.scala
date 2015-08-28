package org.ciroque.ccr

import akka.actor.ActorRefFactory
import org.ciroque.ccr.datastores.SettingsDataStore
import org.ciroque.ccr.stats.AccessStatsClient
import org.slf4j.LoggerFactory
import spray.routing.HttpServiceActor

class CentralConfigurationRepositoryActor(ds: SettingsDataStore, asc: AccessStatsClient)
  extends HttpServiceActor {

  override def actorRefFactory = context

  val configurationProviderService = new ConfigurationProviderService {
    override implicit def actorRefFactory: ActorRefFactory = context

    override implicit val dataStore: SettingsDataStore = ds
    override implicit val accessStatsClient = asc
    override implicit val logger = LoggerFactory.getLogger(classOf[ConfigurationProviderService])
  }

  val configurationManagementService = new ConfigurationManagementService {
    override implicit def actorRefFactory: ActorRefFactory = context

    override implicit val dataStore: SettingsDataStore = ds
  }

  override def receive: Receive = runRoute(configurationManagementService.routes ~ configurationProviderService.routes)
}
