/*
 * Copyright The OpenTelemetry Authors
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

package io.opentelemetry.instrumentation.reactor;

import io.opentelemetry.context.ContextUtils;
import io.opentelemetry.context.Scope;
import io.opentelemetry.trace.Span;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.util.context.Context;

/**
 * Based on Spring Sleuth's Reactor instrumentation. A trace representation of the {@link
 * Subscriber}
 *
 * @author Jose Montoya
 */
public class TracingSubscriber<T> implements CoreSubscriber<T> {
  private final io.grpc.Context traceContext;
  private final Subscriber<? super T> subscriber;
  private final Context context;

  public TracingSubscriber(Subscriber<? super T> subscriber, Context ctx) {
    this(subscriber, ctx, ctx.getOrDefault(Span.class, io.grpc.Context.current()));
  }

  public TracingSubscriber(
      Subscriber<? super T> subscriber, Context ctx, io.grpc.Context contextToPropagate) {
    this.subscriber = subscriber;
    this.traceContext = contextToPropagate;
    this.context = ctx.put(Span.class, this.traceContext);
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    subscriber.onSubscribe(subscription);
  }

  @Override
  public void onNext(T o) {
    withActiveSpan(() -> subscriber.onNext(o));
  }

  @Override
  public void onError(Throwable throwable) {
    withActiveSpan(() -> subscriber.onError(throwable));
  }

  @Override
  public void onComplete() {
    withActiveSpan(subscriber::onComplete);
  }

  @Override
  public Context currentContext() {
    return context;
  }

  private void withActiveSpan(Runnable runnable) {
    if (traceContext != null) {
      try (Scope ignored = ContextUtils.withScopedContext(traceContext)) {
        runnable.run();
      }
    } else {
      runnable.run();
    }
  }
}
