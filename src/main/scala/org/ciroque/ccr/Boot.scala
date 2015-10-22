package org.ciroque.ccr

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import org.ciroque.ccr.core.{Commons, ConfigFactory}
import org.ciroque.ccr.stats.RedisAccessStatsClient
import spray.can.Http

import scala.concurrent.duration._

object Boot extends App {

  import org.slf4j.{Logger, LoggerFactory}

  implicit val system = ActorSystem(Commons.KeyStrings.ActorSystemName)
  implicit val timeout = Timeout(5.seconds)
  val config = ConfigFactory.load("application.conf")
  private val logger: Logger = LoggerFactory.getLogger(Commons.KeyStrings.ActorSystemName)
  private val dataStore = EngineFactory.buildStorageInstance(config)
  private val accessStatsClient = new RedisAccessStatsClient()

  val service = system.actorOf(
    Props(
      new CentralConfigurationRepositoryActor(dataStore, accessStatsClient)),
    "central-configuration-repository-service")

  IO(Http) ? Http.Bind(service, interface = "localhost", port = 8378)
}
