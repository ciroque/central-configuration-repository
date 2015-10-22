package org.ciroque.ccr.core

import org.ciroque.ccr.responses.InternalServerErrorResponse
import spray.http.HttpHeaders.RawHeader
import spray.http.{StatusCode, StatusCodes}
import spray.json._

object Commons {

  import java.util.Locale

  import org.joda.time.format.DateTimeFormat

  val DateTimeFormatter1123 = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'").withZoneUTC().withLocale(Locale.US)

  val rootPath = "ccr"
  val settingsSegment = "settings"
  val schedulingSegment = "schedule"
  val teaPotStatusCode = StatusCodes.registerCustom(418, "I'm a tea pot")
  val corsHeaders = List(
    RawHeader("Access-Control-Allow-Origin", "*"),
    RawHeader("Access-Control-Allow-Headers", "Content-Type"),
    RawHeader("Access-Control-Allow-Methods", "GET,PUT,POST")
  )

  def failureResponseFactory(message: String, cause: Throwable): (JsValue, StatusCode) = {
    import org.ciroque.ccr.responses.InternalServerErrorResponseProtocol._
    (InternalServerErrorResponse(message, cause.getMessage).toJson, StatusCodes.InternalServerError)
  }

  object KeyStrings {
    final val ActorSystemName = "central-configuration-repository-system"
    final val IdKey = "_id"
    final val EnvironmentKey = "environment"
    final val ApplicationKey = "application"
    final val ScopeKey = "scope"
    final val SettingKey = "setting"
    final val EffectiveAtKey = "effectiveAt"
    final val ExpiresAtKey = "expiresAt"
    final val TtlKey = "ttl"
    final val TemporalityKey = "temporality"
    final val TemporalizationKey = "temporalization"
    final val KeyKey = "key"
    final val ValueKey = "value"
    final var SourceIdKey = "sourceId"
  }

  object ApiDocumentationStrings {
    final val ApiDescription = "Centralized repository for application configuration settings."
    final val ApiRoute = "/ccr"
    final val ApiTitle = "Central Configuration Repository"
    final val ApiLicense = ""
    final val ApiLicenseUri = ""
    final val AppRoute = "App Route"
    final val ApplicationsNotes = "Retrieves the list of applications in the given environment."
    final val ApplicationsRoute = "Retrieve Applications"
    final val ConfigurationsNotes = "Retrieves the configuration for the given environment, application, scope, and setting."
    final val ConfigurationProviderApiDescription = "Central Configuration Repository - Configuration Provider"
    final val ConfigurationsRoute = "Retrieve Configuration"
    final val ApiContact = "Steve Wagner (scalawagz@outlook.com)"
    final val EnvironmentsNotes = "Retrieves the list of environments in the system."
    final val EnvironmentsRoute = "Retrieve Environments"
    final val GetMethod = "GET"
    final val PathParamType = "path"
    final val RootRoute = "Root Route"
    final val SeeDocumentation = "Please review the documentation to learn how to use this service."
    final val SettingsNotes = "Retrieves the list of settings for the given environment, application, and scope."
    final val SettingsRoute = "Retrieve Settings"
    final val ScopesNotes = "Retrieves the list of scopes for the given environment and application."
    final val ScopesRoute = "Retrieve Scopes"
    final val StringDataType = "String"
    final val TermsOfServiceUri = ""
  }
}
