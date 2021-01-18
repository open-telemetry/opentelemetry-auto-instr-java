/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spring.batch;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import io.opentelemetry.instrumentation.api.config.Config;
import java.util.List;

public final class SpringBatchInstrumentationConfig {
  private static final List<String> INSTRUMENTATION_NAMES =
      unmodifiableList(asList("spring-batch", "spring-batch-3.0"));

  public static List<String> instrumentationNames() {
    return INSTRUMENTATION_NAMES;
  }

  // the item level instrumentation is very chatty so it's disabled by default
  public static boolean isItemLevelTracingEnabled() {
    return Config.get()
        .isInstrumentationPropertyEnabled(instrumentationNames(), "item.enabled", false);
  }

  public static boolean shouldCreateRootSpanForChunk() {
    return Config.get()
        .isInstrumentationPropertyEnabled(instrumentationNames(), "chunk.root-span", false);
  }

  private SpringBatchInstrumentationConfig() {}
}
