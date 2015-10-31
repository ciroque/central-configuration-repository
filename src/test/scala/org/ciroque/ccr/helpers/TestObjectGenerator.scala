package org.ciroque.ccr.helpers

import java.util.UUID

import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.models.ConfigurationFactory.Configuration
import org.joda.time.DateTime
import spray.json.JsString

object TestObjectGenerator {

  def randomString(length: Int = 10) = scala.util.Random.alphanumeric.take(length).mkString

  def configuration(id: UUID = UUID.randomUUID()): Configuration = ConfigurationFactory(
    id,
    randomString(),
    randomString(),
    randomString(),
    randomString(),
    Some(randomString()),
    JsString(randomString()),
    DateTime.now().minusMinutes(Math.random().toInt),
    DateTime.now().plusMinutes(Math.random().toInt),
    Math.random().toLong
  )
}
