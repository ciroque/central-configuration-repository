package org.ciroque.ccr.models

import org.joda.time.DateTime
import spray.json.{JsString, JsValue, RootJsonFormat, DefaultJsonProtocol}

object Setting extends DefaultJsonProtocol {

  implicit object DateTimeFormatter extends RootJsonFormat[DateTime] {
    override def read(json: JsValue): DateTime = DateTime.parse(json.toString())
    override def write(obj: DateTime): JsValue = JsString(obj.toDateTimeISO.toString())
  }

  implicit val KeyResponseFormat = jsonFormat4(Key.apply)
  implicit val TemporalityResponseFormat = jsonFormat3(Temporality.apply)
  implicit val SettingResponseFormat = jsonFormat3(Setting.apply)

  case class Key(environment: String, application: String, scope: String, setting: String)

  case class Temporality(effectiveAt: DateTime, expiresAt: DateTime, ttl: Long)

  case class Setting(key: Key, value: String, temporality: Temporality)

  def apply(
             environment: String,
             application: String,
             scope: String,
             setting: String,
             value: String,
             effectiveAt: DateTime,
             expiresAt: DateTime,
             ttl: Long) = {
    new Setting(
      new Key(environment, application, scope, setting),
      value,
      new Temporality(effectiveAt, expiresAt, ttl)
    )
  }
}
