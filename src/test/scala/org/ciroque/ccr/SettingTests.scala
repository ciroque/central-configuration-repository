package org.ciroque.ccr

import org.ciroque.ccr.models.ConfigurationFactory
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.{Matchers, FunSpec}
import spray.json.{JsNumber, JsString, JsValue}

class SettingTests extends FunSpec with Matchers {

  describe("Setting ") {

    val effectiveAt = DateTime.now(DateTimeZone.UTC).withZone(DateTimeZone.forID("America/New_York"))
    val expiresAt = effectiveAt.plusMonths(6)

    it("should be constructable via a flattened parameter factory") {
      val setting = ConfigurationFactory("env", "app", "scope", "setting", "1000000", effectiveAt, expiresAt, 5000)
      setting.key.environment should equal( "env")
      setting.key.application should equal( "app")
      setting.key.scope should equal( "scope")
      setting.key.setting should equal( "setting")
      setting.value should equal( "1000000")
      setting.temporality.effectiveAt should equal( effectiveAt)
      setting.temporality.expiresAt should equal( expiresAt)
    }

    it("should be renderable to JSON") {

      def assertValue(map: Map[String, JsValue], key: String, expectedValue: JsValue) =
        map.get(key).map(actualValue => actualValue should equal( expectedValue))

      val configuration = ConfigurationFactory("env", "app", "scope", "setting", "1000000", effectiveAt, expiresAt, 5000)
      val json = configuration.toJson.asJsObject

      val fields = json.fields
      fields.keys should contain("key")
      fields.keys should contain("value")
      fields.keys should contain("temporality")

      assertValue(fields, "value", JsString("1000000"))

      configuration._id.toString should fullyMatch regex "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"

      val keyFields = fields.get("key").get.asJsObject.fields
      keyFields.keys should contain("environment")
      keyFields.keys should contain("application")
      keyFields.keys should contain("scope")
      keyFields.keys should contain("setting")

      assertValue(keyFields, "environment", JsString("env"))
      assertValue(keyFields, "application", JsString("app"))
      assertValue(keyFields, "scope", JsString("scope"))
      assertValue(keyFields, "setting", JsString("setting"))

      val temporalityFields = fields.get("temporality").get.asJsObject.fields
      temporalityFields.keys should contain("effectiveAt")
      temporalityFields.keys should contain("expiresAt")
      temporalityFields.keys should contain("ttl")

      assertValue(temporalityFields, "effectiveAt", JsString(effectiveAt.toString()))
      assertValue(temporalityFields, "expiresAt", JsString(expiresAt.toString()))
      assertValue(temporalityFields, "ttl", JsNumber(5000))
    }
  }
}
