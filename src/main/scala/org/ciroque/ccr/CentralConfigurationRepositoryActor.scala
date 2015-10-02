package org.ciroque.ccr

import akka.actor.ActorRefFactory
import com.gettyimages.spray.swagger.SwaggerHttpService
import com.wordnik.swagger.model.ApiInfo
import org.ciroque.ccr.core.Commons
import org.ciroque.ccr.datastores.SettingsDataStore
import org.ciroque.ccr.stats.AccessStatsClient
import org.slf4j.LoggerFactory
import spray.routing.HttpServiceActor
import scala.reflect.runtime.universe._

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

  val swaggerService = new SwaggerHttpService {
    def actorRefFactory = context
    def apiTypes = Seq(typeOf[ConfigurationProviderService], typeOf[ConfigurationManagementService])
    override def apiInfo = Some(new ApiInfo("Central Configuration Repository", "Centralized repository for application configuration settings.", "", "Steve Wagner (scalawagz@outlook.com)", "", ""))
    def apiVersion = "1.0"
    def baseUrl = s"/${Commons.rootPath}" //the url of your api, not swagger's json endpoint
  }

  def routes = configurationManagementService.routes ~ configurationProviderService.routes ~ swaggerService.routes

  override def receive: Receive = runRoute {
    routes
  }
}
