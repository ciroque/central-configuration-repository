package org.ciroque.ccr

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import org.ciroque.ccr.datastores.InMemoryDataStore
import spray.can.Http

import scala.concurrent.duration._

object Boot extends App {
  implicit val system = ActorSystem("central-configuration-repository-system")
  implicit val timeout = Timeout(5.seconds)
  private val dataStore = new InMemoryDataStore()

  val service = system.actorOf(Props(new CentralConfigurationRepositoryActor(dataStore)), "central-configuration-repository-service")

  IO(Http) ? Http.Bind(service, interface = "localhost", port = 35487)
}
