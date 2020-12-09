/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.tracer;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

// Operation has convenience overloads, Tracer has extension points
// Usage should be consolidated to one or the other
public interface HttpClientOperation<RESPONSE> {

  static <RESPONSE> HttpClientOperation<RESPONSE> noop() {
    return NoopHttpClientOperation.noop();
  }

  static <RESPONSE> HttpClientOperation<RESPONSE> create(
      Context context, Context parentContext, HttpClientTracer<?, ?, RESPONSE> tracer) {
    return new DefaultHttpClientOperation<>(context, parentContext, tracer);
  }

  Scope makeCurrent();

  /** Used for running user callbacks on completion of the http client operation. */
  Scope makeParentCurrent();

  void end(RESPONSE response);

  void end(RESPONSE response, long endTimeNanos);

  void endExceptionally(Throwable t);

  void endExceptionally(RESPONSE response, Throwable throwable);

  void endExceptionally(RESPONSE response, Throwable throwable, long endTimeNanos);

  /** Convenience method for bytecode instrumentation. */
  default void endMaybeExceptionally(RESPONSE response, Throwable throwable) {
    if (throwable != null) {
      endExceptionally(throwable);
    } else {
      end(response);
    }
  }

  Span getSpan();
}
