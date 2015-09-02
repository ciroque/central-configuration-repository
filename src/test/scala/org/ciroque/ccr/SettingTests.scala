package org.ciroque.ccr

import org.ciroque.ccr.models.ConfigurationFactory
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.{FunSpec, Matchers}
import spray.json.{JsNumber, JsString, JsValue}

class SettingTests extends FunSpec with Matchers {

  describe("Setting") {

    val effectiveAt = DateTime.now(DateTimeZone.UTC).withZone(DateTimeZone.forID("America/New_York"))
    val expiresAt = effectiveAt.plusMonths(6)

    val env = "E"
    val app = "A"
    val scp = "S"
    val set = "T"
    val value = "V"
    val ttl = 5000

    it("should be constructable via a flattened parameter factory") {
      val setting = ConfigurationFactory(env, app, scp, set, value, effectiveAt, expiresAt, ttl)
      setting.key.environment should equal(env)
      setting.key.application should equal(app)
      setting.key.scope should equal(scp)
      setting.key.setting should equal(set)
      setting.value should equal(value)
      setting.temporality.effectiveAt should equal(effectiveAt)
      setting.temporality.expiresAt should equal(expiresAt)
    }

    it("should be renderable to JSON") {
      import org.ciroque.ccr.core.Commons

      def assertValue(map: Map[String, JsValue], key: String, expectedValue: JsValue) = {
        map.get(key).foreach(actualValue => actualValue should equal(expectedValue))
      }

      val configuration = ConfigurationFactory(env, app, scp, set, value, effectiveAt, expiresAt, ttl)
      val json = configuration.toJson.asJsObject

      val fields = json.fields
      fields.keys should contain(Commons.KeyStrings.keyKey)
      fields.keys should contain(Commons.KeyStrings.valueKey)
      fields.keys should contain(Commons.KeyStrings.temporalityKey)

      assertValue(fields, Commons.KeyStrings.valueKey, JsString(value))

      configuration._id.toString should fullyMatch regex "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"

      val keyFields = fields.get(Commons.KeyStrings.keyKey).get.asJsObject.fields
      keyFields.keys should contain(Commons.KeyStrings.environmentKey)
      keyFields.keys should contain(Commons.KeyStrings.applicationKey)
      keyFields.keys should contain(Commons.KeyStrings.scopeKey)
      keyFields.keys should contain(Commons.KeyStrings.settingKey)

      assertValue(keyFields, Commons.KeyStrings.environmentKey, JsString(env))
      assertValue(keyFields, Commons.KeyStrings.applicationKey, JsString(app))
      assertValue(keyFields, Commons.KeyStrings.scopeKey, JsString(scp))
      assertValue(keyFields, Commons.KeyStrings.settingKey, JsString(set))

      val temporalityFields = fields.get(Commons.KeyStrings.temporalityKey).get.asJsObject.fields
      temporalityFields.keys should contain(Commons.KeyStrings.effectiveAtKey)
      temporalityFields.keys should contain(Commons.KeyStrings.expiresAtKey)
      temporalityFields.keys should contain(Commons.KeyStrings.ttlKey)

      assertValue(temporalityFields, Commons.KeyStrings.effectiveAtKey, JsString(effectiveAt.toString()))
      assertValue(temporalityFields, Commons.KeyStrings.expiresAtKey, JsString(expiresAt.toString()))
      assertValue(temporalityFields, Commons.KeyStrings.ttlKey, JsNumber(ttl))
    }
  }
}
