package org.ciroque.ccr.datastores

import akka.actor.Actor
import org.ciroque.ccr.datastores.DataStoreResults.Found
import org.ciroque.ccr.datastores.SettingsDataStoreActor._

object SettingsDataStoreActor {
  case class GetEnvironments()
  case class GetApplications(environment: String)
  case class GetScopes(environment: String, application: String)
  case class GetSettings(environment: String, application: String, scope: String)
  case class GetConfiguration(environment: String, application: String, scope: String, setting: String)
}

class SettingsDataStoreActor extends Actor {
  override def receive: Receive = {
    case GetEnvironments() => sender ! Found(List())
    case GetApplications(environment) => sender ! Found(List())
    case GetScopes(environment, application) => sender ! Found(List())
    case GetSettings(environment, application, scope) => sender ! Found(List())
    case GetConfiguration(environment, application, scope, setting) => sender ! Found(List())
  }
}
