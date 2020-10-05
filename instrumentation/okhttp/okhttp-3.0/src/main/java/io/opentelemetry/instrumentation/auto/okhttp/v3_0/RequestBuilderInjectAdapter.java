/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.auto.okhttp.v3_0;

import io.opentelemetry.context.propagation.TextMapPropagator;
import okhttp3.Request;

/**
 * Helper class to inject span context into request headers.
 *
 * @author Pavol Loffay
 */
public class RequestBuilderInjectAdapter implements TextMapPropagator.Setter<Request.Builder> {

  public static final RequestBuilderInjectAdapter SETTER = new RequestBuilderInjectAdapter();

  @Override
  public void set(Request.Builder carrier, String key, String value) {
    carrier.addHeader(key, value);
  }
}
