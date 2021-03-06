package org.ciroque.ccr.responses

import org.ciroque.ccr.models.ConfigurationFactory.Configuration
import spray.json.DefaultJsonProtocol

object ConfigurationResponseProtocol extends DefaultJsonProtocol {
  implicit val ConfigurationResponseFormat = jsonFormat1(ConfigurationResponse)
}

case class ConfigurationResponse(configuration: List[Configuration]) extends CcrResponse
