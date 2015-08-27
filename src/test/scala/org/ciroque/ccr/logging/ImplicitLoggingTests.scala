package org.ciroque.ccr.logging

import org.ciroque.ccr.logging.ImplicitLogging._
import org.scalatest.mock._
import org.scalatest.{FunSpec, Matchers}

class ImplicitLoggingTests extends FunSpec with Matchers with EasyMockSugar {

  describe("ImplicitLogging") {
    it("writes an info message to the logger") {
      implicit val mockLogger = new CachingLogger()
      val implicitLoggerName: String = "IMPLICIT_LOGGING_TESTING"
      withImplicitLogging(implicitLoggerName) {
        implicit val logger = mockLogger

        ()

        addValue("Value1", "TheValueOfOne")
      }

      mockLogger.getEvents.size shouldBe 1
      val firstEvent = mockLogger.getEvents.head

      firstEvent should include(implicitLoggerName)
      firstEvent should include("Value1")
      firstEvent should include("TheValueOfOne")
    }
  }
}
