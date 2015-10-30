package org.ciroque.ccr.responses

import org.ciroque.ccr.models.ConfigurationFactory.Configuration
import spray.json.DefaultJsonProtocol

object BulkConfigurationResponseProtocol extends DefaultJsonProtocol {
  implicit val BulkConfigurationStatusFormat = jsonFormat3(BulkConfigurationStatus)
  implicit val BulkConfigurationInsertResponseFormat = jsonFormat1(BulkConfigurationInsertResponse)
}

case class BulkConfigurationInsertResponse(results: List[BulkConfigurationStatus]) extends CcrResponse {
  def isSuccess = results.forall(r ⇒ r.status == 201)
}

case class BulkConfigurationStatus(status: Int, configuration: Configuration, href: String)
