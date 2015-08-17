package org.ciroque.ccr.models

import java.util.UUID

import org.joda.time.DateTime
import spray.json._

object ConfigurationFactory extends DefaultJsonProtocol {

  implicit object UuidJsonFormat extends JsonFormat[UUID] {
    def write(x: UUID) = JsString(x toString())

    def read(value: JsValue) = value match {
      case JsString(x) => UUID.fromString(x)
      case x => deserializationError("Expected UUID as JsString, but got " + x)
    }
  }

  implicit object DateTimeFormatter extends RootJsonFormat[DateTime] {
    override def read(json: JsValue): DateTime = {
      json match {
        case JsString(string) => DateTime.parse(string)
        case _ => throw new IllegalArgumentException("WTF")
      }
    }

    override def write(obj: DateTime): JsValue = JsString(obj.toString())
  }

  implicit val KeyResponseFormat = jsonFormat4(Key.apply)
  implicit val TemporalityResponseFormat = jsonFormat3(Temporality.apply)
  implicit val SettingResponseFormat = jsonFormat4(Configuration.apply)

  case class Key(environment: String, application: String, scope: String, setting: String)

  case class Temporality(effectiveAt: DateTime, expiresAt: DateTime, ttl: Long)

  case class Configuration(_id: UUID, key: Key, value: String, temporality: Temporality)

  def apply(environment: String,
            application: String,
            scope: String,
            setting: String,
            value: String,
            effectiveAt: DateTime,
            expiresAt: DateTime,
            ttl: Long) = {
    new Configuration(
      UUID.randomUUID(),
      new Key(environment, application, scope, setting),
      value,
      new Temporality(effectiveAt, expiresAt, ttl)
    )
  }
}
