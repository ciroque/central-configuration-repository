package org.ciroque.ccr

import com.redis.RedisClient
import org.ciroque.ccr.stats.RedisAccessStatsClient
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

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
    try {
      val client = new RedisClient("localhost", 6379)
      client.flushall
    } catch {
      case t: Throwable => println(s"ACK: Error connecting to Redis (IGNORING) $t")
    }
  }

  describe("Query Stats") {
    it("adds a non-existent key and increments to 1") {
      accessStatsClient.recordQuery(environment, application, scope, setting) onComplete {
        case Success(v) => v should be(1)
        case Failure(e) => fail(e)
      }
    }
  }
}
