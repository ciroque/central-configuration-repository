package org.ciroque.ccr.datastores

object DataStoreResults {

  trait DataStoreResult

  case class Added[T](item: T) extends DataStoreResult

  case class Errored[T](item: T, message: String) extends DataStoreResult

  case class Deleted[T](item: T) extends DataStoreResult

  case class Failure(message: String, cause: Throwable = null) extends DataStoreResult

  case class Found[T](items: Seq[T]) extends DataStoreResult

  case class NotFound[T](item: Option[T], message: String) extends DataStoreResult

  case class Updated[T](prevItem: T, newItem: T) extends DataStoreResult

}
