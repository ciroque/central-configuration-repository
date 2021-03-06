package org.ciroque.ccr.models

import java.util.UUID

import org.ciroque.ccr.core.CommonJsonFormatters._
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

  object ConfigurationOrdering {
    implicit def ascending: Ordering[Configuration] = Ordering.fromLessThan(
      (l,r) => (l.temporality.effectiveAt isAfter r.temporality.effectiveAt) && l.key.environment != DefaultEnvironment
    )
  }

  val DefaultEnvironment = "default"

  implicit val KeyResponseFormat = jsonFormat5(Key.apply)
  implicit val TemporalityResponseFormat = jsonFormat3(Temporality.apply)
  implicit val SettingResponseFormat = jsonFormat4(Configuration.apply)
  implicit val ConfigurationListFormat = jsonFormat1(ConfigurationList.apply)
  implicit val AuditEntryFormat = jsonFormat3(AuditEntry)
  implicit val AuditHistoryFormat = jsonFormat2(AuditHistory)

  val EmptyConfigurationId = new UUID(0,0)
  val EmptyConfiguration = Configuration(EmptyConfigurationId, Key("", "", "", ""), JsString(""), Temporality(DateTime.now(), DateTime.now(), 0L))

  def apply(environment: String,
            application: String,
            scope: String,
            setting: String,
            value: JsValue,
            effectiveAt: DateTime,
            expiresAt: DateTime,
            ttl: Long): Configuration = {
    apply(UUID.randomUUID(), environment, application, scope, setting, value, effectiveAt, expiresAt, ttl)
  }

  def apply(id: UUID,
            environment: String,
            application: String,
            scope: String,
            setting: String,
            value: JsValue,
            effectiveAt: DateTime,
            expiresAt: DateTime,
            ttl: Long): Configuration = {
    apply(id, environment, application, scope, setting, None, value, effectiveAt, expiresAt, ttl)
  }

  def apply(id: UUID,
            environment: String,
            application: String,
            scope: String,
            setting: String,
            sourceId: Option[String],
            value: JsValue,
            effectiveAt: DateTime,
            expiresAt: DateTime,
            ttl: Long): Configuration = {
    new Configuration(
      id,
      new Key(environment, application, scope, setting, sourceId),
      value,
      new Temporality(effectiveAt, expiresAt, ttl)
    )
  }

  case class Key(environment: String, application: String, scope: String, setting: String, sourceId: Option[String] = None)

  case class Temporality(effectiveAt: DateTime, expiresAt: DateTime, ttl: Long)

  case class ConfigurationList(configurations: List[Configuration])

  case class Configuration(_id: UUID, key: Key, value: JsValue, temporality: Temporality) {
    def isActive = {

      def temporallyActive() = {
        val now = DateTime.now()
        now.isAfter(temporality.effectiveAt) && now.isBefore(temporality.expiresAt)
      }

      key.environment.toLowerCase == DefaultEnvironment || temporallyActive()
    }
  }

  case class AuditEntry(date: DateTime, original: Configuration, updated: Option[Configuration])

  case class AuditHistory(uuid: UUID, events: List[AuditEntry])
}
