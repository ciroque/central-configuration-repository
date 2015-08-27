package org.ciroque.ccr.logging

import org.ciroque.ccr.core.CommonJsonFormatters._
import org.joda.time.{DateTime, DateTimeZone}
import org.slf4j.Logger
import spray.json.{DefaultJsonProtocol, _}

object ImplicitLogging {

  private[logging] val activeLoggers = new ThreadLocal[LogEntryBuilder]

  def getCurrentImplicitLogger = activeLoggers.get

  def withImplicitLogging[R](name: String)(fx: => R)(implicit logger: Logger) = {
    activeLoggers.set(new LogEntryBuilder)
    val started: DateTime = DateTime.now(DateTimeZone.UTC)

    try {
      fx

    } finally {
      val ended = DateTime.now(DateTimeZone.UTC)
      val logEntry = getCurrentImplicitLogger.buildAsJson(name, started, ended)
      logger.info(logEntry.toString())
    }
  }

  def addValue(name: String, value: String) = {
    getCurrentImplicitLogger.addValue(name, value)
  }

  class LogEntryBuilder() {
    var values: List[Values] = List()

    def addValue(name: String, value: String): List[Values] = {
      values = values :+ Values(name, value)
      values
    }

    def build(name: String, started: DateTime, ended: DateTime): LogEntry = {
      LogEntry(name, Timing(started, ended, ended.getMillis - started.getMillis), values)
    }

    def buildAsJson(name: String, started: DateTime, ended: DateTime): JsValue = {
      import LogEntryProtocol._
      build(name, started, ended).toJson
    }
  }

  object ValuesProtocol extends DefaultJsonProtocol {
    implicit def valueFormat: RootJsonFormat[Values] = jsonFormat2(Values)
  }

  object TimingProtocol extends DefaultJsonProtocol {
    implicit def timingFormat: RootJsonFormat[Timing] = jsonFormat3(Timing)
  }

  object LogEntryProtocol extends DefaultJsonProtocol {

    import org.ciroque.ccr.logging.ImplicitLogging.TimingProtocol._
    import org.ciroque.ccr.logging.ImplicitLogging.ValuesProtocol._

    implicit def logEntryFormat: RootJsonFormat[LogEntry] = jsonFormat3(LogEntry)
  }

  case class LogEntry(name: String, timing: Timing, values: List[Values])

  case class Timing(start: DateTime, end: DateTime, duration: Long)

  case class Values(name: String, value: String)

  //  private trait Result(name: String)
  //
  //  private case class Success() extends Result
  //  private case class Error(msg: String) extends Result
  //  private case class Exception(msg: String, cause: Throwable)
}
