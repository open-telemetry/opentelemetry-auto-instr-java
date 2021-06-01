/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rxjava3;

import io.opentelemetry.instrumentation.api.config.Config;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TracingAssemblyActivation {

  private static final ClassValue<AtomicBoolean> activated =
      new ClassValue<AtomicBoolean>() {
        @Override
        protected AtomicBoolean computeValue(Class<?> type) {
          return new AtomicBoolean();
        }
      };

  public static void activate(Class<?> clz) {
    if (activated.get(clz).compareAndSet(false, true)) {
      TracingAssembly.newBuilder()
          .setCaptureExperimentalSpanAttributes(
              Config.get()
                  .getBooleanProperty(
                      "otel.instrumentation.rxjava.experimental-span-attributes", false))
          .build()
          .enable();
    }
  }

  private TracingAssemblyActivation() {}
}
