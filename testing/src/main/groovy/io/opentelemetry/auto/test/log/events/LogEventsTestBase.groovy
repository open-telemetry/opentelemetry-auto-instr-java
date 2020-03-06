/*
 * Copyright 2020, OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.opentelemetry.auto.test.log.events

import io.opentelemetry.OpenTelemetry
import io.opentelemetry.auto.config.Config
import io.opentelemetry.auto.test.AgentTestRunner
import io.opentelemetry.trace.Tracer
import spock.lang.Unroll

import static io.opentelemetry.auto.test.utils.ConfigUtils.withConfigOverride
import static io.opentelemetry.auto.test.utils.TraceUtils.runUnderTrace

/**
 * This class represents the standard test cases that new logging library integrations MUST
 * satisfy in order to support log events.
 */
@Unroll
abstract class LogEventsTestBase extends AgentTestRunner {

  final Tracer tracer = OpenTelemetry.getTracerFactory().get("io.opentelemetry.auto.test")

  abstract Object createLogger(String name)

  String warn() {
    return "warn"
  }

  String error() {
    return "error"
  }

  def "capture #testMethod (#capture)"() {
    setup:
    runUnderTrace("test") {
      def logger = createLogger("abc")
      withConfigOverride(Config.LOG_CAPTURE_THRESHOLD, "WARN") {
        logger."$testMethod"("xyz")
      }
    }

    expect:
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          operationName "test"
          if (capture) {
            event(0) {
              eventName "xyz"
              attributes {
                "level" testMethod.toUpperCase()
                "loggerName" "abc"
              }
            }
          }
          tags {
          }
        }
      }
    }

    where:
    testMethod | capture
    "info"     | false
    warn()     | true
    error()    | true
  }

  def "capture #testMethod (#capture) as span when no current span"() {
    when:
    def logger = createLogger("abc")
    withConfigOverride(Config.LOG_CAPTURE_THRESHOLD, "WARN") {
      withConfigOverride(Config.LOG_CAPTURE_SPAN_ENABLED, "true") {
        logger."$testMethod"("xyz")
      }
    }

    then:
    if (capture) {
      assertTraces(1) {
        trace(0, 1) {
          span(0) {
            operationName "log.message"
            tags {
              "message" "xyz"
              "level" testMethod.toUpperCase()
              "loggerName" "abc"
            }
          }
        }
      }
    } else {
      Thread.sleep(500) // sleep a bit just to make sure no span is captured
      assertTraces(0) {
      }
    }

    where:
    testMethod | capture
    "info"     | false
    warn()     | true
    error()    | true
  }
}
