package org.ciroque.ccr.stats

import com.redis.RedisClient

import scala.concurrent.Future

class RedisAccessStatsClient extends AccessStatsClient {
  val client = new RedisClient("localhost", 6379)

  def recordQuery(environment: String, application: String, scope: String, setting: String): Future[Long] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      client.incr(s"CCR-QUERY:$environment:$application:$scope:$setting") match {
        case Some(value) => value
        case None => -1
      }
    }
  }
}