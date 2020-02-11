package io.opentelemetry.auto.instrumentation.netty41.client;

import static io.netty.handler.codec.http.HttpHeaderNames.HOST;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.auto.decorator.HttpClientDecorator;
import io.opentelemetry.trace.Tracer;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyHttpClientDecorator extends HttpClientDecorator<HttpRequest, HttpResponse> {
  public static final NettyHttpClientDecorator DECORATE = new NettyHttpClientDecorator();

  public static final Tracer TRACER =
      OpenTelemetry.getTracerFactory().get("io.opentelemetry.auto.netty-4.1");

  @Override
  protected String getComponentName() {
    return "netty-client";
  }

  @Override
  protected String method(final HttpRequest httpRequest) {
    return httpRequest.method().name();
  }

  @Override
  protected URI url(final HttpRequest request) throws URISyntaxException {
    final URI uri = new URI(request.uri());
    if ((uri.getHost() == null || uri.getHost().equals("")) && request.headers().contains(HOST)) {
      return new URI("http://" + request.headers().get(HOST) + request.uri());
    } else {
      return uri;
    }
  }

  @Override
  protected String hostname(final HttpRequest request) {
    try {
      final URI uri = new URI(request.uri());
      if ((uri.getHost() == null || uri.getHost().equals("")) && request.headers().contains(HOST)) {
        return request.headers().get(HOST).split(":")[0];
      } else {
        return uri.getHost();
      }
    } catch (final Exception e) {
      return null;
    }
  }

  @Override
  protected Integer port(final HttpRequest request) {
    try {
      final URI uri = new URI(request.uri());
      if ((uri.getHost() == null || uri.getHost().equals("")) && request.headers().contains(HOST)) {
        final String[] hostPort = request.headers().get(HOST).split(":");
        return hostPort.length == 2 ? Integer.parseInt(hostPort[1]) : null;
      } else {
        return uri.getPort();
      }
    } catch (final Exception e) {
      return null;
    }
  }

  @Override
  protected Integer status(final HttpResponse httpResponse) {
    return httpResponse.status().code();
  }
}
