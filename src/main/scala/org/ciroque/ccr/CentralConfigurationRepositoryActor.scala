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

  val configurationProviderService = new ConfigurationProviderService {
    override implicit def actorRefFactory: ActorRefFactory = context

    override implicit val dataStore: SettingsDataStore = ds
    override implicit val accessStatsClient = asc
    override implicit val logger = LoggerFactory.getLogger(classOf[ConfigurationProviderService])
  }
  val configurationSchedulingService = new ConfigurationSchedulingService {
    override implicit def actorRefFactory: ActorRefFactory = context

    override implicit val dataStore: SettingsDataStore = ds
    override implicit val logger = LoggerFactory.getLogger(classOf[ConfigurationProviderService])
  }

  val configurationBulkSchedulingService = new BulkConfigurationSchedulingService {
    override implicit val dataStore: SettingsDataStore = ds
    override implicit def actorRefFactory: ActorRefFactory = context
    override implicit val logger = LoggerFactory.getLogger(classOf[ConfigurationProviderService])
  }

  val configurationAuditingService = new ConfigurationAuditService {
    override implicit val dataStore: SettingsDataStore = ds
    override implicit def actorRefFactory: ActorRefFactory = context
    override implicit val logger = LoggerFactory.getLogger(classOf[ConfigurationProviderService])
  }

  val swaggerService = new SwaggerHttpService {
    def actorRefFactory = context

    def apiTypes = Seq(typeOf[ConfigurationProviderService], typeOf[ConfigurationSchedulingService])

    override def apiInfo = Some(new ApiInfo(
      Commons.ApiDocumentationStrings.ApiTitle,
      Commons.ApiDocumentationStrings.ApiDescription,
      Commons.ApiDocumentationStrings.TermsOfServiceUri,
      Commons.ApiDocumentationStrings.ApiContact,
      Commons.ApiDocumentationStrings.ApiLicense,
      Commons.ApiDocumentationStrings.ApiLicenseUri))

    def apiVersion = "1.0"

    def baseUrl = s"/${Commons.rootPath}" //the url of your api, not swagger's json endpoint
  }

  override def actorRefFactory = context

  override def receive: Receive = runRoute {
    routes
  }

  def routes = configurationSchedulingService.routes ~
    configurationProviderService.routes ~
    configurationBulkSchedulingService.routes ~
    swaggerService.routes ~
    configurationAuditingService.routes
}
