package org.ciroque.ccr.responses

import spray.json.DefaultJsonProtocol

object EnvironmentGetResponseProtocol extends DefaultJsonProtocol {
  implicit val EnvironmentGetResponseFormat = jsonFormat1(EnvironmentGetResponse)
}

object ApplicationGetResponseProtocol extends DefaultJsonProtocol {
  implicit val ApplicationGetResponseFormat = jsonFormat2(ApplicationGetResponse)
}

object ScopeGetResponse extends DefaultJsonProtocol {
  implicit val ScopeGetResponseFormat = jsonFormat3(ScopeGetResponse.apply)

}

object SettingGetResponse extends DefaultJsonProtocol {
  implicit val SettingGetResponseFormat = jsonFormat4(SettingGetResponse.apply)
}

case class EnvironmentGetResponse(environments: List[String]) extends CcrResponse

case class ApplicationGetResponse(environment: String, applications: List[String]) extends CcrResponse

case class ScopeGetResponse(environment: String, application: String, scopes: List[String]) extends CcrResponse

case class SettingGetResponse(environment: String, application: String, scope: String, settings: List[String]) extends CcrResponse
