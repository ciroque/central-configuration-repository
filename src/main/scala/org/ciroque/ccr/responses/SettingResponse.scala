package org.ciroque.ccr.responses

import org.ciroque.ccr.models.ConfigurationFactory.Configuration
import spray.json.DefaultJsonProtocol

object SettingResponse extends DefaultJsonProtocol {
  implicit val SettingResponseFormat = jsonFormat1(SettingResponse.apply)
}

case class SettingResponse(setting: List[Configuration]) extends CcrResponse
