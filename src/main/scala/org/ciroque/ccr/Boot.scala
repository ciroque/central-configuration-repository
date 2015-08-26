package org.ciroque.ccr

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import org.ciroque.ccr.datastores.InMemorySettingsDataStore
import spray.can.Http
import stats.RedisAccessStatsClient

import scala.concurrent.duration._

object Boot extends App {
  implicit val system = ActorSystem("central-configuration-repository-system")
  implicit val timeout = Timeout(5.seconds)

  // TODO: Use a factory and configuration to new these up.
  private val dataStore = new InMemorySettingsDataStore()
  private val accessStatsClient = new RedisAccessStatsClient()

  val service = system.actorOf(
    Props(
      new CentralConfigurationRepositoryActor(dataStore, accessStatsClient)),
    "central-configuration-repository-service")

  IO(Http) ? Http.Bind(service, interface = "localhost", port = 35487)
}
