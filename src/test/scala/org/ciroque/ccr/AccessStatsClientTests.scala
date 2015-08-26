package org.ciroque.ccr

import com.redis.RedisClient
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import stats.RedisAccessStatsClient

class AccessStatsClientTests
  extends FunSpec
  with Matchers
  with ScalaFutures
  with BeforeAndAfterAll {

  val accessStatsClient = new RedisAccessStatsClient()

  val environment = "test-env"
  val application = "test-app"
  val scope = "test-scope"
  val setting = "test-setting"

  override def beforeAll(): Unit = {
    val client = new RedisClient("localhost", 6379)
    client.flushall
  }

  describe("Query Stats") {
    it("adds a non-existent key and increments to 1") {
      whenReady(accessStatsClient.recordQuery(environment, application, scope, setting)) {
        value => value should be(1)
      }
    }
  }
}
