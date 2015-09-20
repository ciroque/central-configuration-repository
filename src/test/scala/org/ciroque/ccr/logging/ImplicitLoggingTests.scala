package org.ciroque.ccr.logging

import org.ciroque.ccr.logging.ImplicitLogging._
import org.scalatest.mock._
import org.scalatest.{FunSpec, Matchers}

class ImplicitLoggingTests extends FunSpec with Matchers with EasyMockSugar {

  describe("ImplicitLogging") {
    it("writes an info message to the logger with Success result") {
      implicit val mockLogger = new CachingLogger()
      val implicitLoggerName: String = "IMPLICIT_LOGGING_TESTING"
      withImplicitLogging(implicitLoggerName) {
        implicit val logger = mockLogger

        ()

        recordValue("Value1", "TheValueOfOne")
      }

      mockLogger.getEvents.size shouldBe 1
      val firstEvent = mockLogger.getEvents.head

      firstEvent should include(implicitLoggerName)
      firstEvent should include("Value1")
      firstEvent should include("TheValueOfOne")
      firstEvent should include("SUCCESS")

      mockLogger.printLog()
    }

    it("writes an info message to the logger with Error result") {
      implicit val mockLogger = new CachingLogger()
      val implicitLoggerName: String = "IMPLICIT_LOGGING_TESTING"
      withImplicitLogging(implicitLoggerName) {
        implicit val logger = mockLogger

        ()

        recordValue("Value1", "TheValueOfOne")
        setResultError("ERROR_MESSAGE")
      }

      mockLogger.getEvents.size shouldBe 1
      val firstEvent = mockLogger.getEvents.head

      firstEvent should include(implicitLoggerName)
      firstEvent should include("Value1")
      firstEvent should include("TheValueOfOne")
      firstEvent should include("ERROR")
      firstEvent should include("ERROR_MESSAGE")

      mockLogger.printLog()
    }

    it("writes an info message to the logger with Exception result") {
      implicit val mockLogger = new CachingLogger()
      val implicitLoggerName: String = "IMPLICIT_LOGGING_TESTING"

      withImplicitLogging(implicitLoggerName) {
        try {
          implicit val logger = mockLogger

          recordValue("Value1", "TheValueOfOne")

          throw new IllegalArgumentException("AN_EXCEPTION_MESSAGE")
        } catch {
          case ex: Throwable => setResultException(ex)
        }
      }

      mockLogger.getEvents.size shouldBe 1
      val firstEvent = mockLogger.getEvents.head

      firstEvent should include(implicitLoggerName)
      firstEvent should include("Value1")
      firstEvent should include("TheValueOfOne")
      firstEvent should include("EXCEPTION")
      firstEvent should include("AN_EXCEPTION_MESSAGE")

      mockLogger.printLog()
    }
  }
}
