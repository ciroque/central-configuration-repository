package org.ciroque.ccr.stats

import com.redis.RedisClient

import scala.concurrent.Future

class RedisAccessStatsClient extends AccessStatsClient {
  val ERROR_CODE = -1
  def recordQuery(environment: String, application: String, scope: String, setting: String): Future[Long] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      val client = new RedisClient("localhost", 6379)
      client.incr(s"CCR-QUERY:$environment:$application:$scope:$setting") match {
        case Some(value) => value
        case None => ERROR_CODE
      }
    }.recoverWith { case t: Throwable => Future.successful(ERROR_CODE) }
  }
}