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
    val value = Left("V")
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
      fields.keys should contain(Commons.KeyStrings.KeyKey)
      fields.keys should contain(Commons.KeyStrings.ValueKey)
      fields.keys should contain(Commons.KeyStrings.TemporalityKey)

      assertValue(fields, Commons.KeyStrings.ValueKey, JsString(value.a))

      configuration._id.toString should fullyMatch regex "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"

      val keyFields = fields.get(Commons.KeyStrings.KeyKey).get.asJsObject.fields
      keyFields.keys should contain(Commons.KeyStrings.EnvironmentKey)
      keyFields.keys should contain(Commons.KeyStrings.ApplicationKey)
      keyFields.keys should contain(Commons.KeyStrings.ScopeKey)
      keyFields.keys should contain(Commons.KeyStrings.SettingKey)

      assertValue(keyFields, Commons.KeyStrings.EnvironmentKey, JsString(env))
      assertValue(keyFields, Commons.KeyStrings.ApplicationKey, JsString(app))
      assertValue(keyFields, Commons.KeyStrings.ScopeKey, JsString(scp))
      assertValue(keyFields, Commons.KeyStrings.SettingKey, JsString(set))

      val temporalityFields = fields.get(Commons.KeyStrings.TemporalityKey).get.asJsObject.fields
      temporalityFields.keys should contain(Commons.KeyStrings.EffectiveAtKey)
      temporalityFields.keys should contain(Commons.KeyStrings.ExpiresAtKey)
      temporalityFields.keys should contain(Commons.KeyStrings.TtlKey)

      assertValue(temporalityFields, Commons.KeyStrings.EffectiveAtKey, JsString(effectiveAt.toString()))
      assertValue(temporalityFields, Commons.KeyStrings.ExpiresAtKey, JsString(expiresAt.toString()))
      assertValue(temporalityFields, Commons.KeyStrings.TtlKey, JsNumber(ttl))
    }
  }
}
