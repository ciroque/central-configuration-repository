package org.ciroque.ccr

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import org.ciroque.ccr.datastores.InMemorySettingsDataStore
import org.ciroque.ccr.stats.RedisAccessStatsClient
import spray.can.Http

import scala.concurrent.duration._

object Boot extends App {

  import org.slf4j.{LoggerFactory, Logger}

  private val actorSystemName: String = "central-configuration-repository-system"
  implicit val system = ActorSystem(actorSystemName)
  implicit val timeout = Timeout(5.seconds)

  private val logger: Logger = LoggerFactory.getLogger(actorSystemName)

  // TODO: Use a factory and configuration to new these up.
  private val dataStore = new InMemorySettingsDataStore()(logger)
  private val accessStatsClient = new RedisAccessStatsClient()

  val service = system.actorOf(
    Props(
      new CentralConfigurationRepositoryActor(dataStore, accessStatsClient)),
    "central-configuration-repository-service")

  IO(Http) ? Http.Bind(service, interface = "localhost", port = 35487)
}
