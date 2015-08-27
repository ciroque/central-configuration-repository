package org.ciroque.ccr.stats

import scala.concurrent.Future

trait AccessStatsClient {
  def recordQuery(environment: String, application: String, scope: String, setting: String): Future[Long]
}
