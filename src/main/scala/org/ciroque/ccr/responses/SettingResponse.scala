package org.ciroque.ccr.responses

import org.ciroque.ccr.models.SettingFactory.Setting
import spray.json.DefaultJsonProtocol

object SettingResponse extends DefaultJsonProtocol {
  implicit val SettingResponseFormat = jsonFormat1(SettingResponse.apply)
}

case class SettingResponse(setting: Option[Setting]) extends CcrResponse
