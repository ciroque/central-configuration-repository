package org.ciroque.ccr

import org.ciroque.ccr.core.{Commons, CcrService}
import org.ciroque.ccr.datastores.SettingsDataStore
import spray.routing.HttpService
import spray.routing._
import spray.http._


trait ConfigurationBulkSchedulingService
  extends HttpService
  with CcrService {

  implicit val dataStore: SettingsDataStore

  def rootBulkRoute = path(Commons.rootPath / Commons.schedulingSegment / Commons.bulkSegment) {
    pathEndOrSingleSlash {
      complete("")
    }
  }

  def routes = rootBulkRoute
}
