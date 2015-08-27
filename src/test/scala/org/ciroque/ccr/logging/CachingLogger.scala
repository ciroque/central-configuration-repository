package org.ciroque.ccr.logging

import java.util.concurrent.ConcurrentLinkedQueue

import org.slf4j.{Marker, Logger}

class CachingLogger extends Logger {

  private def asListOfStrings = cache.toArray(new Array[String](cache.size())).toList

  def printLog(): Unit = println(asListOfStrings mkString "\n")

  def reset(): Unit = cache.clear()

  private val cache: ConcurrentLinkedQueue[String] = new ConcurrentLinkedQueue[String]()

  def getEvents: List[String] = asListOfStrings

  override def getName: String = "CachingLogger"

  override def warn(s: String): Unit = ???

  override def warn(s: String, o: scala.Any): Unit = ???

  override def warn(s: String, objects: AnyRef*): Unit = ???

  override def warn(s: String, o: scala.Any, o1: scala.Any): Unit = ???

  override def warn(s: String, throwable: Throwable): Unit = ???

  override def warn(marker: Marker, s: String): Unit = ???

  override def warn(marker: Marker, s: String, o: scala.Any): Unit = ???

  override def warn(marker: Marker, s: String, o: scala.Any, o1: scala.Any): Unit = ???

  override def warn(marker: Marker, s: String, objects: AnyRef*): Unit = ???

  override def warn(marker: Marker, s: String, throwable: Throwable): Unit = ???

  override def isErrorEnabled: Boolean = ???

  override def isErrorEnabled(marker: Marker): Boolean = ???

  override def isInfoEnabled: Boolean = ???

  override def isInfoEnabled(marker: Marker): Boolean = ???

  override def isDebugEnabled: Boolean = ???

  override def isDebugEnabled(marker: Marker): Boolean = ???

  override def isTraceEnabled: Boolean = ???

  override def isTraceEnabled(marker: Marker): Boolean = ???

  override def error(s: String): Unit = ???

  override def error(s: String, o: scala.Any): Unit = ???

  override def error(s: String, o: scala.Any, o1: scala.Any): Unit = ???

  override def error(s: String, objects: AnyRef*): Unit = ???

  override def error(s: String, throwable: Throwable): Unit = ???

  override def error(marker: Marker, s: String): Unit = ???

  override def error(marker: Marker, s: String, o: scala.Any): Unit = ???

  override def error(marker: Marker, s: String, o: scala.Any, o1: scala.Any): Unit = ???

  override def error(marker: Marker, s: String, objects: AnyRef*): Unit = ???

  override def error(marker: Marker, s: String, throwable: Throwable): Unit = ???

  override def debug(s: String): Unit = ???

  override def debug(s: String, o: scala.Any): Unit = ???

  override def debug(s: String, o: scala.Any, o1: scala.Any): Unit = ???

  override def debug(s: String, objects: AnyRef*): Unit = ???

  override def debug(s: String, throwable: Throwable): Unit = ???

  override def debug(marker: Marker, s: String): Unit = ???

  override def debug(marker: Marker, s: String, o: scala.Any): Unit = ???

  override def debug(marker: Marker, s: String, o: scala.Any, o1: scala.Any): Unit = ???

  override def debug(marker: Marker, s: String, objects: AnyRef*): Unit = ???

  override def debug(marker: Marker, s: String, throwable: Throwable): Unit = ???

  override def isWarnEnabled: Boolean = ???

  override def isWarnEnabled(marker: Marker): Boolean = ???

  override def trace(s: String): Unit = ???

  override def trace(s: String, o: scala.Any): Unit = ???

  override def trace(s: String, o: scala.Any, o1: scala.Any): Unit = ???

  override def trace(s: String, objects: AnyRef*): Unit = ???

  override def trace(s: String, throwable: Throwable): Unit = ???

  override def trace(marker: Marker, s: String): Unit = ???

  override def trace(marker: Marker, s: String, o: scala.Any): Unit = ???

  override def trace(marker: Marker, s: String, o: scala.Any, o1: scala.Any): Unit = ???

  override def trace(marker: Marker, s: String, objects: AnyRef*): Unit = ???

  override def trace(marker: Marker, s: String, throwable: Throwable): Unit = ???

  override def info(s: String): Unit = cache.add(s)

  override def info(s: String, o: scala.Any): Unit = ???

  override def info(s: String, o: scala.Any, o1: scala.Any): Unit = ???

  override def info(s: String, objects: AnyRef*): Unit = ???

  override def info(s: String, throwable: Throwable): Unit = ???

  override def info(marker: Marker, s: String): Unit = ???

  override def info(marker: Marker, s: String, o: scala.Any): Unit = ???

  override def info(marker: Marker, s: String, o: scala.Any, o1: scala.Any): Unit = ???

  override def info(marker: Marker, s: String, objects: AnyRef*): Unit = ???

  override def info(marker: Marker, s: String, throwable: Throwable): Unit = ???
}
