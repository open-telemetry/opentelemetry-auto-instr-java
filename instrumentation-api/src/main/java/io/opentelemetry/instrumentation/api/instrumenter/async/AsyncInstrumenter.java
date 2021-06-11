/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter.async;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;

/**
 * A wrapper over {@link Instrumenter} that is able to defer {@link Instrumenter#end(Context,
 * Object, Object, Throwable)} until asynchronous computation finishes.
 *
 * <p>This class is not able to extract {@code RESPONSE} from the asynchronous computation value it
 * receives in the {@link #asyncEnd(Context, Object, Object, Throwable)} call, so it will always
 * pass {@code null} as the response to the wrapped {@link Instrumenter}.
 */
public final class AsyncInstrumenter<REQUEST> {

  /**
   * Returns a new {@link AsyncInstrumenter} that wraps over passed {@code syncInstrumenter},
   * configured for usage with asynchronous computations that are instances of {@code asyncType}.
   */
  public static <REQUEST> AsyncInstrumenter<REQUEST> create(
      Instrumenter<REQUEST, ?> syncInstrumenter, Class<?> asyncType) {
    return new AsyncInstrumenter<>(
        syncInstrumenter, asyncType, AsyncEndStrategies.resolveStrategy(asyncType));
  }

  private final Instrumenter<REQUEST, ?> instrumenter;
  private final Class<?> asyncType;
  private final AsyncOperationEndStrategy asyncOperationEndStrategy;

  private AsyncInstrumenter(
      Instrumenter<REQUEST, ?> instrumenter,
      Class<?> asyncType,
      AsyncOperationEndStrategy asyncOperationEndStrategy) {
    this.instrumenter = instrumenter;
    this.asyncType = asyncType;
    this.asyncOperationEndStrategy = asyncOperationEndStrategy;
  }

  /** Same as calling {@link Instrumenter#shouldStart(Context, Object)}. */
  public boolean shouldStart(Context parentContext, REQUEST request) {
    return instrumenter.shouldStart(parentContext, request);
  }

  /** Same as calling {@link Instrumenter#start(Context, Object)}. */
  public Context start(Context parentContext, REQUEST request) {
    return instrumenter.start(parentContext, request);
  }

  /**
   * Attempts to compose over passed {@code asyncValue} and delay the {@link
   * Instrumenter#end(Context, Object, Object, Throwable)} call until the async operation completes.
   *
   * <p>This method will end the operation immediately if {@code throwable} is passed, if there is
   * no {@link AsyncOperationEndStrategy} for the {@code asyncType} used, or if there is a type
   * mismatch between passed {@code asyncValue} and the {@code asyncType} that was used to create
   * this object.
   *
   * <p>If the passed {@code asyncValue} is recognized as an asynchronous computation, the operation
   * won't be {@link Instrumenter#end(Context, Object, Object, Throwable) ended} until {@code
   * asyncValue} completes.
   */
  public <ASYNC> ASYNC asyncEnd(
      Context context, REQUEST request, ASYNC asyncValue, Throwable throwable) {
    // we can end early if an exception was thrown
    if (throwable != null) {
      instrumenter.end(context, request, null, throwable);
      return asyncValue;
    }

    // use the configured strategy to compose over the asyncValue
    if (asyncOperationEndStrategy != null && asyncType.isInstance(asyncValue)) {
      return (ASYNC) asyncOperationEndStrategy.end(instrumenter, context, request, asyncValue);
    }

    // fall back to sync end() if asyncValue type doesn't match
    instrumenter.end(context, request, null, null);
    return asyncValue;
  }
}
