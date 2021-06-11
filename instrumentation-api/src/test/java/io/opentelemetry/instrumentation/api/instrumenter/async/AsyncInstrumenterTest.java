/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncInstrumenterTest {
  @Mock Instrumenter<String, String> instrumenter;

  @Test
  void shouldEndImmediatelyWhenExceptionWasPassed() {
    // given
    AsyncInstrumenter<String> underTest =
        AsyncInstrumenter.create(instrumenter, CompletableFuture.class);

    Context context = Context.root();
    Exception exception = new RuntimeException("boom!");
    CompletableFuture<String> future = new CompletableFuture<>();

    // when
    CompletableFuture<String> result = underTest.asyncEnd(context, "request", future, exception);

    // then
    assertSame(future, result);

    verify(instrumenter).end(context, "request", null, exception);
  }

  @Test
  void shouldEndImmediatelyWhenWrongReturnTypeWasPassed() {
    // given
    AsyncInstrumenter<String> underTest = AsyncInstrumenter.create(instrumenter, Future.class);

    Context context = Context.root();
    CompletableFuture<String> future = new CompletableFuture<>();

    // when
    CompletableFuture<String> result = underTest.asyncEnd(context, "request", future, null);

    // then
    assertSame(future, result);

    verify(instrumenter).end(context, "request", null, null);
  }

  @Test
  void shouldEndImmediatelyWhenAsyncWrapperisOfWrongType() {
    // given
    AsyncInstrumenter<String> underTest =
        AsyncInstrumenter.create(instrumenter, CompletableFuture.class);

    Context context = Context.root();

    // when
    String result = underTest.asyncEnd(context, "request", "not async", null);

    // then
    assertSame("not async", result);

    verify(instrumenter).end(context, "request", null, null);
  }

  @Test
  void shouldReturnedDecoratedAsyncWrapper() {
    // given
    AsyncInstrumenter<String> underTest =
        AsyncInstrumenter.create(instrumenter, CompletionStage.class);

    Context context = Context.root();
    CompletableFuture<String> future = new CompletableFuture<>();

    // when
    CompletableFuture<String> result = underTest.asyncEnd(context, "request", future, null);

    // then
    assertNotSame(future, result);
    verifyNoInteractions(instrumenter);

    // when
    future.complete("done!");

    // then
    assertEquals("done!", result.join());
    verify(instrumenter).end(context, "request", null, null);
  }
}
