package org.ciroque.ccr

import org.ciroque.ccr.models.SettingFactory
import org.joda.time.{DateTimeZone, DateTime}
import org.specs2.mutable.Specification
import spray.json.{JsNumber, JsValue, JsString}

class SettingSpec extends Specification {

  "Setting " should {

    val effectiveAt = DateTime.now(DateTimeZone.UTC).withZone(DateTimeZone.forID("America/New_York"))
    val expiresAt = effectiveAt.plusMonths(6)

    "be constructable via a flattened parameter factory" in {
      val setting = SettingFactory("env", "app", "scope", "setting", "1000000", effectiveAt, expiresAt, 5000)
      setting.key.environment must_== "env"
      setting.key.application must_== "app"
      setting.key.scope must_== "scope"
      setting.key.setting must_== "setting"
      setting.value must_== "1000000"
      setting.temporality.effectiveAt must_== effectiveAt
      setting.temporality.expiresAt must_== expiresAt
    }

    "be renderable to JSON" in {

      def assertValue(map: Map[String, JsValue], key: String, expectedValue: JsValue) = {
        map.get(key).map(actualValue => actualValue must_== expectedValue)
      }

      val setting = SettingFactory("env", "app", "scope", "setting", "1000000", effectiveAt, expiresAt, 5000)
      val json = setting.toJson.asJsObject

      val fields = json.fields
      fields.keys must contain("key")
      fields.keys must contain("value")
      fields.keys must contain("temporality")

      assertValue(fields, "value", JsString("1000000"))

      val keyFields = fields.get("key").get.asJsObject.fields
      keyFields.keys must contain("environment")
      keyFields.keys must contain("application")
      keyFields.keys must contain("scope")
      keyFields.keys must contain("setting")

      assertValue(keyFields, "environment", JsString("env"))
      assertValue(keyFields, "application", JsString("app"))
      assertValue(keyFields, "scope", JsString("scope"))
      assertValue(keyFields, "setting", JsString("setting"))

      val temporalityFields = fields.get("temporality").get.asJsObject.fields
      temporalityFields.keys must contain("effectiveAt")
      temporalityFields.keys must contain("expiresAt")
      temporalityFields.keys must contain("ttl")

//      assertValue(temporalityFields, "effectiveAt", JsString(effectiveAt.toString))
//      assertValue(temporalityFields, "expiresAt", JsString(expiresAt.toString))
      assertValue(temporalityFields, "ttl", JsNumber(5000))

      true must_== true
    }
  }
}
