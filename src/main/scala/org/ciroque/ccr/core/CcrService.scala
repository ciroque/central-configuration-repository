package org.ciroque.ccr.core

import org.ciroque.ccr.SemanticVersion

trait CcrService {
  def getVersion: SemanticVersion
}
