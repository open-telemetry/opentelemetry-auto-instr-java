/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.netty.v3_8.server;

import static io.opentelemetry.javaagent.instrumentation.netty.v3_8.server.NettyHttpServerTracer.TRACER;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.instrumentation.api.ContextStore;
import io.opentelemetry.javaagent.instrumentation.netty.v3_8.ChannelTraceContext;
import io.opentelemetry.trace.Span;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;
import org.jboss.netty.handler.codec.http.HttpResponse;

public class HttpServerResponseTracingHandler extends SimpleChannelDownstreamHandler {

  private final ContextStore<Channel, ChannelTraceContext> contextStore;

  public HttpServerResponseTracingHandler(ContextStore<Channel, ChannelTraceContext> contextStore) {
    this.contextStore = contextStore;
  }

  @Override
  public void writeRequested(ChannelHandlerContext ctx, MessageEvent msg) {
    ChannelTraceContext channelTraceContext =
        contextStore.putIfAbsent(ctx.getChannel(), ChannelTraceContext.Factory.INSTANCE);

    Context context = TRACER.getServerContext(channelTraceContext);
    if (context == null || !(msg.getMessage() instanceof HttpResponse)) {
      ctx.sendDownstream(msg);
      return;
    }

    Span span = Span.fromContext(context);
    try (Scope ignored = context.makeCurrent()) {
      ctx.sendDownstream(msg);
    } catch (Throwable throwable) {
      TRACER.endExceptionally(span, throwable);
      throw throwable;
    }
    TRACER.end(span, (HttpResponse) msg.getMessage());
  }
}
