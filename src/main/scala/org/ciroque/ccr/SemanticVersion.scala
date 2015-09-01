package org.ciroque.ccr

import spray.json.DefaultJsonProtocol

object SemanticVersionProtocol extends DefaultJsonProtocol {
  implicit def semanticVersionFormat = jsonFormat3(SemanticVersion)
}

case class SemanticVersion(major: Int, minor: Int, patch: Int)
