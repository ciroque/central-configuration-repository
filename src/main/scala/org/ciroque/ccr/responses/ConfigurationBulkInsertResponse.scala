package org.ciroque.ccr.responses

import org.ciroque.ccr.core.Commons
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.models.ConfigurationFactory.Configuration
import spray.json.DefaultJsonProtocol

object BulkConfigurationResponseProtocol extends DefaultJsonProtocol {
  implicit val BulkConfigurationStatusFormat = jsonFormat5(BulkConfigurationStatus)
  implicit val BulkConfigurationInsertResponseFormat = jsonFormat1(BulkConfigurationResponse.apply)
}

case class BulkConfigurationResponse(results: List[BulkConfigurationStatus]) extends CcrResponse {
  def getStatusCode = {
    val distinct = results.groupBy(_.status).keys
    distinct.toList.length match {
      case 0 ⇒ 204
      case 1 ⇒ distinct.head
      case _ ⇒ 207
    }
  }
}

object BulkConfigurationResponse {
  final def FailedBulkConfigurationResponse(message: String): BulkConfigurationResponse =
    BulkConfigurationResponse(List(BulkConfigurationStatusFactory.FailedBulkConfigurationStatus(message)))
}

case class BulkConfigurationStatus(
  status: Int,
  configuration: Configuration,
  updated: Option[Configuration] = None,
  href: String,
  message: Option[String] = None) {

  updated match {
    case None ⇒ ()
    case Some(o) ⇒
      assert(o._id == configuration._id, s"Configuration Ids do not match. L: ${o.key}, R: ${configuration.key}")
  }
}

object BulkConfigurationStatusFactory {

  final def FailedBulkConfigurationStatus(message: String) = apply(500, message)

  def apply(
    status: Int,
    configuration: Configuration,
    other: Option[Configuration],
    href: String,
    message: Option[String]): BulkConfigurationStatus = {

    BulkConfigurationStatus(status, configuration, other, href, message)
  }

  def apply(status: Int, msg: String): BulkConfigurationStatus = {
    apply(status, ConfigurationFactory.EmptyConfiguration, None, "", Some(msg))
  }

  def apply(status: Int, configuration: Configuration): BulkConfigurationStatus = {
    apply(status, configuration, None, buildHref(configuration), None)
  }

  def apply(status: Int, configuration: Configuration, message: String): BulkConfigurationStatus = {
    apply(status, configuration, None, buildHref(configuration), Some(message))
  }

  def apply(status: Int, configuration: Configuration, other: Configuration): BulkConfigurationStatus = {
    apply(status, configuration, Some(other), buildHref(configuration), None)
  }

  def buildHref(configuration: Configuration): String = {
    val key = configuration.key
    val baseHref = s"/${Commons.rootPath}/${Commons.settingsSegment}/${key.environment}/${key.application}/${key.scope}/${key.setting}"
    key.sourceId match {
      case None ⇒ baseHref
      case Some(value) ⇒ s"$baseHref?${Commons.KeyStrings.SourceIdKey}=$value"
    }
  }
}