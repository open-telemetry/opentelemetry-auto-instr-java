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

package io.opentelemetry.auto.instrumentation.lettuce.v5_2;

import io.grpc.Context;
import io.lettuce.core.tracing.TraceContext;
import io.lettuce.core.tracing.TraceContextProvider;
import io.lettuce.core.tracing.Tracer;
import io.lettuce.core.tracing.TracerProvider;
import io.lettuce.core.tracing.Tracing;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.TracingContextUtils;
import io.opentelemetry.trace.attributes.SemanticAttributes;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import reactor.util.annotation.Nullable;

public enum OpenTelemetryTracing implements Tracing {
  INSTANCE;

  public static final io.opentelemetry.trace.Tracer TRACER =
      OpenTelemetry.getTracerProvider().get("io.opentelemetry.auto.lettuce-5.2");

  @Override
  public TracerProvider getTracerProvider() {
    return OpenTelemetryTracerProvider.INSTANCE;
  }

  @Override
  public TraceContextProvider initialTraceContextProvider() {
    return OpenTelemetryTraceContextProvider.INSTANCE;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public boolean includeCommandArgsInSpanTags() {
    return true;
  }

  @Override
  public Endpoint createEndpoint(SocketAddress socketAddress) {
    if (socketAddress instanceof InetSocketAddress) {
      InetSocketAddress address = (InetSocketAddress) socketAddress;

      return new OpenTelemetryEndpoint(
          address.getAddress().getHostAddress(), address.getPort(), address.getHostString());
    }
    return null;
  }

  enum OpenTelemetryTracerProvider implements TracerProvider {
    INSTANCE;

    private final Tracer openTelemetryTracer = new OpenTelemetryTracer();

    @Override
    public Tracer getTracer() {
      return openTelemetryTracer;
    }
  }

  enum OpenTelemetryTraceContextProvider implements TraceContextProvider {
    INSTANCE;

    @Override
    public TraceContext getTraceContext() {
      return new OpenTelemetryTraceContext();
    }
  }

  static class OpenTelemetryTraceContext implements TraceContext {
    private final Context context;

    protected OpenTelemetryTraceContext() {
      this.context = Context.current();
    }

    public Context getContext() {
      return context;
    }
  }

  static class OpenTelemetryEndpoint implements Endpoint {
    final String ip;
    final int port;
    @Nullable final String name;

    protected OpenTelemetryEndpoint(String ip, int port, @Nullable String name) {
      this.ip = ip;
      this.port = port;
      if (!ip.equals(name)) {
        this.name = name;
      } else {
        this.name = null;
      }
    }
  }

  static class OpenTelemetryTracer extends Tracer {

    protected OpenTelemetryTracer() {}

    @Override
    public OpenTelemetrySpan nextSpan() {
      return new OpenTelemetrySpan(TRACER.getCurrentSpan());
    }

    @Override
    public OpenTelemetrySpan nextSpan(TraceContext traceContext) {
      if (!(traceContext instanceof OpenTelemetryTraceContext)) {
        return nextSpan();
      }

      Context context = ((OpenTelemetryTraceContext) traceContext).getContext();

      final io.opentelemetry.trace.Span parent = TracingContextUtils.getSpan(context);

      return new OpenTelemetrySpan(parent);
    }
  }

  // The order that callbacks will be called in or which thread they are called from is not well
  // defined. We go ahead and buffer all data until we know we have a span. This implementation is
  // particularly safe, synchronizing all accesses. Relying on implementation details would allow
  // reducing synchronization but the impact should be minimal.
  static class OpenTelemetrySpan extends Tracer.Span {
    private final Span.Builder spanBuilder;

    @Nullable private String name;

    @Nullable private List<Object> events;

    @Nullable private Status status;

    @Nullable private Span span;

    protected OpenTelemetrySpan(Span parent) {
      // Name will be updated later, we create with an arbitrary one here to store other data before
      // the span starts.
      spanBuilder =
          TRACER
              .spanBuilder("REDIS")
              .setSpanKind(Kind.CLIENT)
              .setParent(parent)
              .setAttribute(SemanticAttributes.DB_TYPE.key(), "redis");
    }

    @Override
    public synchronized Tracer.Span name(String name) {
      if (span != null) {
        span.updateName(name);
      } else {
        this.name = name;
      }

      return this;
    }

    @Override
    public synchronized Tracer.Span remoteEndpoint(Endpoint endpoint) {
      if (endpoint instanceof OpenTelemetryEndpoint) {
        if (span != null) {
          fillEndpoint(span, (OpenTelemetryEndpoint) endpoint);
        } else {
          fillEndpoint(spanBuilder, (OpenTelemetryEndpoint) endpoint);
        }
      }
      return this;
    }

    @Override
    public synchronized Tracer.Span start() {
      span = spanBuilder.startSpan();
      if (name != null) {
        span.updateName(name);
      }

      if (events != null) {
        for (int i = 0; i < events.size(); i += 2) {
          span.addEvent((String) events.get(i), (long) events.get(i + 1));
        }
        events = null;
      }

      if (status != null) {
        span.setStatus(status);
        status = null;
      }

      return this;
    }

    @Override
    public synchronized Tracer.Span annotate(String value) {
      if (span != null) {
        span.addEvent(value);
      } else {
        if (events == null) {
          events = new ArrayList<>();
        }
        events.add(value);
        final Instant now = Instant.now();
        events.add(TimeUnit.SECONDS.toNanos(now.getEpochSecond()) + now.getNano());
      }
      return this;
    }

    @Override
    public synchronized Tracer.Span tag(String key, String value) {
      key = translateTagKey(key);
      if (span != null) {
        span.setAttribute(key, value);
      } else {
        spanBuilder.setAttribute(key, value);
      }
      return this;
    }

    @Override
    public synchronized Tracer.Span error(Throwable throwable) {
      // TODO(anuraaga): Check if any lettuce exceptions map well to a Status and try mapping.
      final Status status =
          Status.UNKNOWN.withDescription(throwable.getClass() + ": " + throwable.getMessage());
      if (span != null) {
        span.setStatus(status);
      } else {
        this.status = status;
      }
      return this;
    }

    @Override
    public synchronized void finish() {
      if (span != null) {
        span.end();
      }
    }

    private static String translateTagKey(String key) {
      switch (key) {
        case "redis.args":
          return SemanticAttributes.DB_STATEMENT.key();
        default:
          return key;
      }
    }

    private static void fillEndpoint(Span.Builder span, OpenTelemetryEndpoint endpoint) {
      span.setAttribute(SemanticAttributes.NET_TRANSPORT.key(), "IP.TCP");
      span.setAttribute(SemanticAttributes.NET_PEER_IP.key(), endpoint.ip);

      final StringBuilder redisUrl = new StringBuilder("redis://");

      if (endpoint.name != null) {
        span.setAttribute(SemanticAttributes.NET_PEER_NAME.key(), endpoint.name);
        redisUrl.append(endpoint.name);
      } else {
        redisUrl.append(endpoint.ip);
      }

      if (endpoint.port != 0) {
        span.setAttribute(SemanticAttributes.NET_PEER_PORT.key(), endpoint.port);
        redisUrl.append(":").append(endpoint.port);
      }

      span.setAttribute(SemanticAttributes.DB_URL.key(), redisUrl.toString());
    }

    private static void fillEndpoint(Span span, OpenTelemetryEndpoint endpoint) {
      span.setAttribute(SemanticAttributes.NET_TRANSPORT.key(), "IP.TCP");
      span.setAttribute(SemanticAttributes.NET_PEER_IP.key(), endpoint.ip);

      final StringBuilder redisUrl = new StringBuilder("redis://");

      if (endpoint.name != null) {
        span.setAttribute(SemanticAttributes.NET_PEER_NAME.key(), endpoint.name);
        redisUrl.append(endpoint.name);
      } else {
        redisUrl.append(endpoint.ip);
      }

      if (endpoint.port != 0) {
        span.setAttribute(SemanticAttributes.NET_PEER_PORT.key(), endpoint.port);
        redisUrl.append(":").append(endpoint.port);
      }

      span.setAttribute(SemanticAttributes.DB_URL.key(), redisUrl.toString());
    }
  }
}
