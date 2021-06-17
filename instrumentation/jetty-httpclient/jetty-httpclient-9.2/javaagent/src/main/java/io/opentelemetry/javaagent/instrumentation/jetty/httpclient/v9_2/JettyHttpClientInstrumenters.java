package io.opentelemetry.javaagent.instrumentation.jetty.httpclient.v9_2;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.javaagent.instrumentation.api.instrumenter.PeerServiceAttributesExtractor;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;

public class JettyHttpClientInstrumenters {

  private static final Instrumenter<Request, Response> INSTRUMENTER;

  private JettyHttpClientInstrumenters() {}

  static {
    JettyClientInstrumenterBuilder builder =
        new JettyClientInstrumenterBuilder(GlobalOpenTelemetry.get());

    PeerServiceAttributesExtractor<Request, Response> peerServiceAttributesExtractor =
        PeerServiceAttributesExtractor.create(new JettyHttpClientNetAttributesExtractor());
    INSTRUMENTER = builder.addAttributeExtractor(peerServiceAttributesExtractor).build();
  }

  public static Instrumenter<Request, Response> instrumenter() {
    return INSTRUMENTER;
  }
}
