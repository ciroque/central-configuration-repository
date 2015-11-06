package org.ciroque.ccr.helpers

import java.util.UUID

import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.models.ConfigurationFactory.{AuditEntry, Configuration}
import org.joda.time.DateTime
import spray.json.JsString

import scala.util.Random

object TestObjectGenerator {

  def randomString(length: Int = 10) = scala.util.Random.alphanumeric.take(length).mkString

  def randomJsString(length: Int = 25) = JsString(randomString(length))
  
  def randomEffectiveAt: DateTime = DateTime.now().minusMinutes(Random.nextInt(60) + 2)

  def randomExpiresAt: DateTime = DateTime.now().plusDays(30)

  def randomInt: Int = Random.nextInt(200)

  def configuration(id: UUID = UUID.randomUUID()): Configuration = ConfigurationFactory(
    id,
    randomString(),
    randomString(),
    randomString(),
    randomString(),
    Some(randomString()),
    randomJsString(),
    randomEffectiveAt,
    randomExpiresAt,
    Math.random().toLong
  )
  
  def configuration(environment: String, application: String, scope: String, setting: String): Configuration = {
    ConfigurationFactory(
      UUID.randomUUID(),
      environment,
    application,
    scope,
    setting,
    None,
    randomJsString(),
    randomEffectiveAt,
    randomExpiresAt,
    randomInt
    )
  }

  def configuration(environment: String,
                     application: String,
                     scope: String,
                     setting: String,
                     effectiveAt: DateTime,
                     expiresAt: DateTime): Configuration = {
    ConfigurationFactory(
      UUID.randomUUID(),
      environment,
    application,
    scope,
    setting,
    None,
    randomJsString(),
    effectiveAt,
    expiresAt,
    randomInt
    )
  }

  def auditHistoryList(uuid: UUID = UUID.randomUUID()): List[AuditEntry] = {
    val now = DateTime.now
    val firstDate = now.minusMonths(6)
    val secondDate = now.minusMonths(4)
    val thirdDate = now.minusMonths(2)
    val firstHistory = (TestObjectGenerator.configuration(uuid), None)
    val secondHistory = (TestObjectGenerator.configuration(uuid), Some(TestObjectGenerator.configuration(uuid)))
    val thirdHistory = (TestObjectGenerator.configuration(uuid), Some(TestObjectGenerator.configuration(uuid)))
    val auditHistory = List(
      AuditEntry(firstDate, firstHistory._1, firstHistory._2),
      AuditEntry(secondDate, secondHistory._1, secondHistory._2),
      AuditEntry(thirdDate, thirdHistory._1, thirdHistory._2)
    )

    auditHistory
  }
}
