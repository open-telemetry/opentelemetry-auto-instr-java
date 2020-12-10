/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.tracer;

import static io.opentelemetry.api.trace.Span.Kind.CLIENT;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.attributes.SemanticAttributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.instrumentation.api.tracer.utils.NetPeerUtils;
import io.opentelemetry.instrumentation.api.tracer.utils.SpanAttributeSetter;
import java.net.URI;
import java.net.URISyntaxException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HttpClientTracer<REQUEST, RESPONSE> extends BaseTracer {

  private static final Logger log = LoggerFactory.getLogger(HttpClientTracer.class);

  public static final String DEFAULT_SPAN_NAME = "HTTP request";

  private static final String USER_AGENT = "User-Agent";

  protected abstract String method(REQUEST request);

  @Nullable
  protected abstract URI url(REQUEST request) throws URISyntaxException;

  @Nullable
  protected String flavor(REQUEST request) {
    // This is de facto standard nowadays, so let us use it, unless overridden
    return "1.1";
  }

  protected abstract Integer status(RESPONSE response);

  @Nullable
  protected abstract String requestHeader(REQUEST request, String name);

  @Nullable
  protected abstract String responseHeader(RESPONSE response, String name);

  protected HttpClientTracer() {
    super();
  }

  protected HttpClientTracer(Tracer tracer) {
    super(tracer);
  }

  /**
   * Convenience overload for {@link #startOperation(Object, Object, TextMapPropagator.Setter,
   * long)} which is applicable when the {@link TextMapPropagator.Setter} applies directly to the
   * {@code request}, and which uses the current time.
   */
  protected Operation startOperation(REQUEST request, TextMapPropagator.Setter<REQUEST> setter) {
    return startOperation(request, request, setter, -1);
  }

  /**
   * Convenience overload for {@link #startOperation(Object, Object, TextMapPropagator.Setter,
   * long)} which is applicable when the {@link TextMapPropagator.Setter} applies directly to the
   * {@code request}.
   */
  protected Operation startOperation(
      REQUEST request, TextMapPropagator.Setter<REQUEST> setter, long startTimeNanos) {
    return startOperation(request, request, setter, startTimeNanos);
  }

  /**
   * Convenience overload for {@link #startOperation(Object, Object, TextMapPropagator.Setter,
   * long)} which uses the current time.
   */
  protected <CARRIER> Operation startOperation(
      REQUEST request, CARRIER carrier, TextMapPropagator.Setter<CARRIER> setter) {
    return startOperation(request, carrier, setter, -1);
  }

  protected <CARRIER> Operation startOperation(
      REQUEST request,
      CARRIER carrier,
      TextMapPropagator.Setter<CARRIER> setter,
      long startTimeNanos) {
    Context parentContext = Context.current();
    if (inClientSpan(parentContext)) {
      return Operation.noop();
    }
    String spanName = spanName(request);
    SpanBuilder spanBuilder = spanBuilder(parentContext, request, spanName, startTimeNanos);
    Context context = withClientSpan(parentContext, spanBuilder.startSpan());
    OpenTelemetry.getGlobalPropagators().getTextMapPropagator().inject(context, carrier, setter);
    return Operation.create(context, parentContext);
  }

  private SpanBuilder spanBuilder(
      Context parentContext, REQUEST request, String spanName, long startTimeNanos) {
    SpanBuilder spanBuilder =
        tracer.spanBuilder(spanName).setSpanKind(CLIENT).setParent(parentContext);
    if (startTimeNanos > 0) {
      spanBuilder.setStartTimestamp(startTimeNanos, NANOSECONDS);
    }
    onRequest(spanBuilder, request);
    return spanBuilder;
  }

  /**
   * This is a helper method for HttpClientTracers that need to implement their {@code
   * startOperation} from scratch.
   */
  protected void onRequest(SpanBuilder spanBuilder, REQUEST request) {
    onRequest(spanBuilder::setAttribute, request);
  }

  /**
   * This is for HttpClientTracers that do not have the request available during {@code
   * startOperation}.
   */
  protected void onRequest(Operation operation, REQUEST request) {
    onRequest(operation.getSpan()::setAttribute, request);
  }

  /** Convenience method for {@link #end(Operation, Object, long)} which uses the current time. */
  public void end(Operation operation, RESPONSE response) {
    end(operation, response, -1);
  }

  public void end(Operation operation, RESPONSE response, long endTimeNanos) {
    // TODO (trask) require response to be non-null here?
    onResponse(operation, response);
    super.end(operation.getSpan(), endTimeNanos);
  }

  /**
   * Convenience method for {@link #endExceptionally(Operation, Throwable, Object, long)} which has
   * no request, and uses the current time.
   */
  public void endExceptionally(Operation operation, Throwable throwable) {
    checkNotNull(throwable);
    endExceptionally(operation, throwable, null, -1);
  }

  /**
   * Convenience method for {@link #endExceptionally(Operation, Throwable, Object, long)} which uses
   * the current time.
   */
  public void endExceptionally(Operation operation, Throwable throwable, RESPONSE response) {
    endExceptionally(operation, throwable, response, -1);
  }

  public void endExceptionally(
      Operation operation, Throwable throwable, RESPONSE response, long endTimeNanos) {
    Span span = operation.getSpan();
    if (response != null) {
      onResponse(operation, response);
    }
    super.endExceptionally(span, throwable, endTimeNanos);
  }

  /** Convenience method primarily for bytecode instrumentation. */
  public void endMaybeExceptionally(Operation operation, RESPONSE response, Throwable throwable) {
    if (throwable != null) {
      endExceptionally(operation, throwable);
    } else {
      end(operation, response);
    }
  }

  /** Can be overridden to capture additional attributes from the response. */
  protected void onResponse(Operation operation, RESPONSE response) {
    // TODO (trask) require response to be non-null here?
    Integer status = status(response);
    if (status != null) {
      Span span = operation.getSpan();
      span.setAttribute(SemanticAttributes.HTTP_STATUS_CODE, (long) status);
      StatusCode statusCode = HttpStatusConverter.statusFromHttpStatus(status);
      if (statusCode == StatusCode.ERROR) {
        span.setStatus(statusCode);
      }
    }
  }

  protected String spanName(REQUEST request) {
    // TODO (trask) require request to be non-null here?
    if (request == null) {
      return DEFAULT_SPAN_NAME;
    }
    String method = method(request);
    return method != null ? "HTTP " + method : DEFAULT_SPAN_NAME;
  }

  private void onRequest(SpanAttributeSetter span, REQUEST request) {
    // TODO (trask) require request to be non-null here?
    if (request != null) {
      span.setAttribute(SemanticAttributes.NET_TRANSPORT, "IP.TCP");
      span.setAttribute(SemanticAttributes.HTTP_METHOD, method(request));
      span.setAttribute(SemanticAttributes.HTTP_USER_AGENT, requestHeader(request, USER_AGENT));
      setFlavor(span, request);
      setUrl(span, request);
    }
  }

  private void setFlavor(SpanAttributeSetter span, REQUEST request) {
    String flavor = flavor(request);
    if (flavor == null) {
      return;
    }
    String httpProtocolPrefix = "HTTP/";
    if (flavor.startsWith(httpProtocolPrefix)) {
      flavor = flavor.substring(httpProtocolPrefix.length());
    }
    span.setAttribute(SemanticAttributes.HTTP_FLAVOR, flavor);
  }

  private void setUrl(SpanAttributeSetter span, REQUEST request) {
    try {
      URI url = url(request);
      if (url != null) {
        NetPeerUtils.INSTANCE.setNetPeer(span, url.getHost(), null, url.getPort());
        span.setAttribute(SemanticAttributes.HTTP_URL, url.toString());
      }
    } catch (Exception e) {
      // TODO why is catch needed here?
      log.debug("Error tagging url", e);
    }
  }

  private static void checkNotNull(Object obj) {
    if (obj == null) {
      throw new NullPointerException();
    }
  }
}
