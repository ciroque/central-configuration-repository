package org.ciroque.ccr.logging

import org.ciroque.ccr.core.CommonJsonFormatters._
import org.joda.time.{DateTime, DateTimeZone}
import org.slf4j.Logger
import spray.json.{DefaultJsonProtocol, _}

import scala.util.DynamicVariable

object ImplicitLogging {

  private[logging] val activeLoggers = new DynamicVariable[LogEntryBuilder](new LogEntryBuilder)

  def withImplicitLogging[R](name: String)(fx: => R)(implicit logger: Logger) = {
//    activeLoggers.set(new LogEntryBuilder)
    val started: DateTime = DateTime.now(DateTimeZone.UTC)

    try {
      fx
    } finally {
      val ended = DateTime.now(DateTimeZone.UTC)
      val logEntry = getCurrentImplicitLogger.buildAsJson(name, started, ended)
      logger.info(logEntry.toString())
    }
  }

  def setResultException(cause: Throwable) = {
    getCurrentImplicitLogger.result = Exception(cause.getMessage, cause)
  }

  def setResultError(msg: String) = {
    getCurrentImplicitLogger.result = Error(msg)
  }

  def getCurrentImplicitLogger = activeLoggers.value

  def recordValue(name: String, value: String) = {
    getCurrentImplicitLogger.addValue(name, value)
  }

  trait Result

  class LogEntryBuilder() {
    var values: List[Values] = List()
    var result: Result = Success()

    def addValue(name: String, value: String): List[Values] = {
      values = values :+ Values(name, value)
      values
    }

    def buildAsJson(name: String, started: DateTime, ended: DateTime): JsValue = {
      import org.ciroque.ccr.logging.ImplicitLogging.LogEntryProtocol._
      build(name, started, ended).toJson
    }

    def build(name: String, started: DateTime, ended: DateTime): LogEntry = {
      LogEntry(name, Timing(started, ended, ended.getMillis - started.getMillis), values, result)
    }
  }

  case class LogEntry(name: String, timing: Timing, values: List[Values], result: Result = Success())

  case class Timing(start: DateTime, end: DateTime, duration: Long)

  case class Values(name: String, value: String)

  case class Success() extends Result

  case class Error(msg: String) extends Result

  case class Exception(msg: String, cause: Throwable) extends Result

  object ValuesProtocol extends DefaultJsonProtocol {
    implicit def valueFormat: RootJsonFormat[Values] = jsonFormat2(Values)
  }

  object TimingProtocol extends DefaultJsonProtocol {
    implicit def timingFormat: RootJsonFormat[Timing] = jsonFormat3(Timing)
  }

  implicit object ResultJsonFormat extends RootJsonFormat[Result] {
    override def read(json: JsValue): Result = {
      val jso = json.asJsObject
      jso.fields.size match {
        case 0 => Success()
        case 1 => Error(jso.fields("msg").toString())
        case 2 => Exception(jso.fields("msg").toString(), null)
      }
    }

    override def write(result: Result): JsValue = {
      result match {
        case success: Success => JsObject("status" -> JsString("SUCCESS"))
        case error: Error => JsObject("status" -> JsString("ERROR"), "message" -> JsString(error.msg))
        case exception: Exception =>
          JsObject(
            "status" -> JsString("EXCEPTION"),
            "message" -> JsString(exception.cause.getMessage),
            "stackTrace" -> JsString(exception.cause.getStackTrace.mkString))
      }
    }
  }

  object LogEntryProtocol extends DefaultJsonProtocol {

    import org.ciroque.ccr.logging.ImplicitLogging.TimingProtocol._
    import org.ciroque.ccr.logging.ImplicitLogging.ValuesProtocol._

    implicit def logEntryFormat: RootJsonFormat[LogEntry] = jsonFormat4(LogEntry)
  }

}
