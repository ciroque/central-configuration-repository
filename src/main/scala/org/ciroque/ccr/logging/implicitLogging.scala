package org.ciroque.ccr.logging

import org.ciroque.ccr.core.CommonJsonFormatters._
import org.joda.time.{DateTime, DateTimeZone}
import org.slf4j.Logger
import spray.json.{DefaultJsonProtocol, _}

object ImplicitLogging {
  def withImplicitLogging[R](name: String)(implicit logger: Logger)(fx: () => R) = {

    val started: DateTime = DateTime.now(DateTimeZone.UTC)

    implicit val builder: LogEntryBuilder = new LogEntryBuilder()

    try {

      fx()

    } finally {


      val ended = DateTime.now(DateTimeZone.UTC)
      val logEntry: LogEntry = builder.build(name, started, ended)



      logger.info(logEntry.toString())
    }
  }

  def addValue(name: String, value: String)(implicit builder: LogEntryBuilder) = {
    builder.addValue(name, value)
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
  }

  object ValuesProtocol extends DefaultJsonProtocol {
    implicit def valueFormat = jsonFormat2(Values)
  }

  object TimingProtocol extends DefaultJsonProtocol {
    implicit def timingFormat = jsonFormat3(Timing)
  }

  object LogEntryProtocol extends DefaultJsonProtocol {

    import TimingProtocol._
    import ValuesProtocol._

    implicit def logEntryFormat = jsonFormat3(LogEntry)
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
