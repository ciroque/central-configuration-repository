package org.ciroque.ccr.core

import org.joda.time.DateTime
import spray.json.{JsString, JsValue, RootJsonFormat}

object CommonJsonFormatters {

  implicit object DateTimeFormatter extends RootJsonFormat[DateTime] {
    override def read(json: JsValue): DateTime = {
      json match {
        case JsString(string) => DateTime.parse(string)
        case _ => throw new IllegalArgumentException("WTF")
      }
    }

    override def write(obj: DateTime): JsValue = JsString(obj.toString())
  }

}
