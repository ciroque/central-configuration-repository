package org.ciroque.ccr.responses

import org.ciroque.ccr.models.ConfigurationFactory.Configuration
import spray.json.DefaultJsonProtocol

object ConfigurationResponseProtocol extends DefaultJsonProtocol {
  implicit val SettingResponseFormat = jsonFormat1(ConfigurationResponse)
}

case class ConfigurationResponse(setting: List[Configuration]) extends CcrResponse
