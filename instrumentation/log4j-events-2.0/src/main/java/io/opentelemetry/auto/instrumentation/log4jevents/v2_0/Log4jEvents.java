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
package io.opentelemetry.auto.instrumentation.log4jevents.v2_0;

import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.auto.config.Config;
import io.opentelemetry.trace.AttributeValue;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;

@Slf4j
public class Log4jEvents {

  private static final Tracer TRACER =
      OpenTelemetry.getTracerFactory().get("io.opentelemetry.auto.log4j-events-2.0");

  public static void capture(
      final Logger logger, final Level level, final Message message, final Throwable t) {

    if (level.intLevel() > getThreshold().intLevel()) {
      return;
    }
    final Span currentSpan = TRACER.getCurrentSpan();
    if (!currentSpan.getContext().isValid()) {
      return;
    }

    final Map<String, AttributeValue> attributes = new HashMap<>(t == null ? 2 : 3);
    attributes.put("level", newAttributeValue(level.toString()));
    attributes.put("loggerName", newAttributeValue(logger.getName()));
    if (t != null) {
      attributes.put("error.stack", newAttributeValue(toString(t)));
    }
    currentSpan.addEvent(message.getFormattedMessage(), attributes);
  }

  private static AttributeValue newAttributeValue(final String stringValue) {
    return AttributeValue.stringAttributeValue(stringValue);
  }

  private static String toString(final Throwable t) {
    final StringWriter out = new StringWriter();
    t.printStackTrace(new PrintWriter(out));
    return out.toString();
  }

  private static Level getThreshold() {
    final String level = Config.get().getLogCaptureThreshold();
    if (level == null) {
      return Level.OFF;
    }
    switch (level) {
      case "OFF":
        return Level.OFF;
      case "FATAL":
        return Level.FATAL;
      case "ERROR":
      case "SEVERE":
        return Level.ERROR;
      case "WARN":
      case "WARNING":
        return Level.WARN;
      case "INFO":
        return Level.INFO;
      case "CONFIG":
      case "DEBUG":
      case "FINE":
      case "FINER":
        return Level.DEBUG;
      case "TRACE":
      case "FINEST":
        return Level.TRACE;
      case "ALL":
        return Level.ALL;
      default:
        log.error("unexpected value for {}: {}", Config.LOG_CAPTURE_THRESHOLD, level);
        return Level.OFF;
    }
  }
}
