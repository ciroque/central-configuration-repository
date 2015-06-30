package org.ciroque.ccr

import org.ciroque.ccr.models.Setting
import org.joda.time.{DateTimeZone, DateTime}
import org.specs2.mutable.Specification

class SettingSpec extends Specification {

  "Setting " should {

    val effectiveAt = DateTime.now(DateTimeZone.UTC).withZone(DateTimeZone.forID("America/New_York"))
    val expiresAt = effectiveAt.plusMonths(6)

    "be constructable via a flattened parameter factory" in {
      val setting = Setting("env", "app", "scope", "setting", "1000000", effectiveAt, expiresAt, 5000)
      setting.key.environment must_== "env"
      setting.key.application must_== "app"
      setting.key.scope must_== "scope"
      setting.key.setting must_== "setting"
      setting.value must_== "1000000"
      setting.temporality.effectiveAt must_== effectiveAt
      setting.temporality.expiresAt must_== expiresAt
    }

    "be renderable to JSON" in {
      val setting = Setting("env", "app", "scope", "setting", "1000000", effectiveAt, expiresAt, 5000)
      val json = setting.toJson.asJsObject
      val fields = json.fields
      fields.keys must contain("key")
      fields.keys must contain("value")
      fields.keys must contain("temporality")

      val keyFields = fields.get("key").get.asJsObject.fields
      keyFields.keys must contain("environment")
      keyFields.keys must contain("application")
      keyFields.keys must contain("scope")
      keyFields.keys must contain("setting")

      val value = fields.get("value").get
      value.toString must_== "\"1000000\""

      val temporalityFields = fields.get("temporality").get.asJsObject.fields
      temporalityFields.keys must contain("effectiveAt")
      temporalityFields.keys must contain("expiresAt")
      temporalityFields.keys must contain("ttl")
    }
  }
}
