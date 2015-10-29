package org.ciroque.ccr.responses

import org.ciroque.ccr.models.ConfigurationFactory.Configuration
import spray.json.DefaultJsonProtocol

object ConfigurationUpdateResponseProtocol extends DefaultJsonProtocol {
  implicit val ConfigurationUpdateResponseFormat = jsonFormat2(ConfigurationUpdateResponse)

}

case class ConfigurationUpdateResponse(previous: Configuration, updated: Configuration) extends CcrResponse
