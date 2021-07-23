/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.results;

import java.util.Map;

public interface ResultsPersister {
  void write(Map<String, AppPerfResults> results);
}
