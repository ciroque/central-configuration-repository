package org.ciroque.ccr.responses

import spray.json.DefaultJsonProtocol

object EnvironmentGetResponse extends DefaultJsonProtocol {
  implicit val EnvironmentGetResponseFormat = jsonFormat1(EnvironmentGetResponse.apply)
}

object ApplicationGetResponse extends DefaultJsonProtocol {
  implicit val ApplicationGetResponseFormat = jsonFormat1(ApplicationGetResponse.apply)
}

object ScopeGetResponse extends DefaultJsonProtocol {
  implicit val ScopeGetResponseFormat = jsonFormat1(ScopeGetResponse.apply)

}

object SettingGetResponse extends DefaultJsonProtocol {
  implicit val SettingGetResponseFormat = jsonFormat1(SettingGetResponse.apply)
}

case class EnvironmentGetResponse(environments: List[String]) extends CcrResponse

case class ApplicationGetResponse(applications: List[String]) extends CcrResponse

case class ScopeGetResponse(scopes: List[String]) extends CcrResponse

case class SettingGetResponse(settings: List[String]) extends CcrResponse
