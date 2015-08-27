package org.ciroque.ccr.logging

import org.scalatest.{Matchers, FunSpec}
import org.slf4j.Logger
import org.easymock.EasyMock._
import org.scalatest.mock._
import ImplicitLogging._

class ImplicitLoggingTests extends FunSpec with Matchers with EasyMockSugar {


  describe("ImplicitLogging") {
    it("writes an info message to the logger") {
      implicit val mockLogger: Logger = mock[Logger]
      expecting {
        mockLogger.info(isA(classOf[String])).andReturn(Unit)
      }
      whenExecuting(mockLogger) {
        withImplicitLogging("IMPLICIT_LOGGING_TESTING") {
          implicit val logger = mockLogger
          println(s"This is something for the logger to test")
        }
      }
    }
  }
}
