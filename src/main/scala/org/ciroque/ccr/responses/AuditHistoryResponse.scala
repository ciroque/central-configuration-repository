package org.ciroque.ccr.responses

import org.ciroque.ccr.models.ConfigurationFactory.AuditEntry
import spray.json.DefaultJsonProtocol

object AuditHistoryResponseProtocol extends DefaultJsonProtocol {
  implicit val AuditHistoryFormat = jsonFormat1(AuditHistoryResponse)
}

case class AuditHistoryResponse(history: List[AuditEntry])
