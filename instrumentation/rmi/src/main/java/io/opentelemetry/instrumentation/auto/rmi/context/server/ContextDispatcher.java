/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.auto.rmi.context.server;

import static io.opentelemetry.instrumentation.api.decorator.BaseDecorator.extract;
import static io.opentelemetry.instrumentation.auto.api.rmi.ThreadLocalContext.THREAD_LOCAL_CONTEXT;
import static io.opentelemetry.instrumentation.auto.rmi.context.ContextPayload.GETTER;
import static io.opentelemetry.instrumentation.auto.rmi.context.ContextPropagator.CONTEXT_CALL_ID;
import static io.opentelemetry.instrumentation.auto.rmi.context.ContextPropagator.PROPAGATOR;
import static io.opentelemetry.trace.TracingContextUtils.getSpan;

import io.grpc.Context;
import io.opentelemetry.instrumentation.auto.rmi.context.ContextPayload;
import io.opentelemetry.trace.SpanContext;
import java.io.IOException;
import java.io.ObjectInput;
import java.rmi.Remote;
import java.rmi.server.RemoteCall;
import sun.rmi.server.Dispatcher;
import sun.rmi.transport.Target;

/**
 * ContextDispatcher is responsible for handling both initial context propagation check call and
 * following call which carries payload
 *
 * <p>Context propagation check is only expected not to throw any exception, hinting to the client
 * that its communicating with an instrumented server. Non instrumented server would've thrown
 * UnknownObjectException
 *
 * <p>Because caching of the result after first call on a connection, only payload calls are
 * expected
 */
public class ContextDispatcher implements Dispatcher {
  private static final ContextDispatcher CONTEXT_DISPATCHER = new ContextDispatcher();
  private static final NoopRemote NOOP_REMOTE = new NoopRemote();

  public static Target newDispatcherTarget() {
    return new Target(NOOP_REMOTE, CONTEXT_DISPATCHER, NOOP_REMOTE, CONTEXT_CALL_ID, false);
  }

  @Override
  public void dispatch(Remote obj, RemoteCall call) throws IOException {
    ObjectInput in = call.getInputStream();
    int operationId = in.readInt();
    in.readLong(); // skip 8 bytes

    if (PROPAGATOR.isOperationWithPayload(operationId)) {
      ContextPayload payload = ContextPayload.read(in);
      if (payload != null) {
        Context context = extract(payload, GETTER);
        SpanContext spanContext = getSpan(context).getContext();
        if (spanContext.isValid()) {
          THREAD_LOCAL_CONTEXT.set(context);
        } else {
          THREAD_LOCAL_CONTEXT.set(null);
        }
      }
    }

    // send result stream the client is expecting
    call.getResultStream(true);

    // release held streams to allow next call to continue
    call.releaseInputStream();
    call.releaseOutputStream();
    call.done();
  }

  public static class NoopRemote implements Remote {}
}
