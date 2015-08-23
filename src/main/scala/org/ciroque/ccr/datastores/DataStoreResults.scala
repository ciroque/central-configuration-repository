package org.ciroque.ccr.datastores

object DataStoreResults {
  trait DataStoreResult

  case class Added[T](item: T) extends DataStoreResult
  case class Found[T](items: Seq[T]) extends DataStoreResult
  case class NotFound(message: String) extends DataStoreResult
  case class Failure(message: String, cause: Throwable = null) extends DataStoreResult
}
