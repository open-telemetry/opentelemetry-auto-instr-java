package io.opentelemetry.auto.instrumentation.elasticsearch6;

import static io.opentelemetry.auto.instrumentation.elasticsearch.ElasticsearchTransportClientDecorator.DECORATE;
import static io.opentelemetry.auto.instrumentation.elasticsearch.ElasticsearchTransportClientDecorator.TRACER;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import io.opentelemetry.auto.instrumentation.api.SpanWithScope;
import io.opentelemetry.auto.tooling.Instrumenter;
import io.opentelemetry.trace.Span;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;

/**
 * Most of this class is identical to version 5's instrumentation, but they changed an interface to
 * an abstract class, so the bytecode isn't directly compatible.
 */
@AutoService(Instrumenter.class)
public class Elasticsearch6TransportClientInstrumentation extends Instrumenter.Default {

  public Elasticsearch6TransportClientInstrumentation() {
    super("elasticsearch", "elasticsearch-transport", "elasticsearch-transport-6");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    // If we want to be more generic, we could instrument the interface instead:
    // .and(safeHasSuperType(named("org.elasticsearch.client.ElasticsearchClient"))))
    return not(isInterface()).and(named("org.elasticsearch.client.support.AbstractClient"));
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      "com.google.common.base.Preconditions",
      "com.google.common.base.Joiner",
      "com.google.common.base.Joiner$1",
      "com.google.common.base.Joiner$2",
      "com.google.common.base.Joiner$MapJoiner",
      "io.opentelemetry.auto.decorator.BaseDecorator",
      "io.opentelemetry.auto.decorator.ClientDecorator",
      "io.opentelemetry.auto.decorator.DatabaseClientDecorator",
      "io.opentelemetry.auto.instrumentation.elasticsearch.ElasticsearchTransportClientDecorator",
      packageName + ".TransportActionListener",
    };
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        isMethod()
            .and(named("execute"))
            .and(takesArgument(0, named("org.elasticsearch.action.Action")))
            .and(takesArgument(1, named("org.elasticsearch.action.ActionRequest")))
            .and(takesArgument(2, named("org.elasticsearch.action.ActionListener"))),
        Elasticsearch6TransportClientInstrumentation.class.getName()
            + "$Elasticsearch6TransportClientAdvice");
  }

  public static class Elasticsearch6TransportClientAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static SpanWithScope onEnter(
        @Advice.Argument(0) final Action action,
        @Advice.Argument(1) final ActionRequest actionRequest,
        @Advice.Argument(value = 2, readOnly = false)
            ActionListener<ActionResponse> actionListener) {

      final Span span = TRACER.spanBuilder("elasticsearch.query").startSpan();
      DECORATE.afterStart(span);
      DECORATE.onRequest(span, action.getClass(), actionRequest.getClass());

      actionListener = new TransportActionListener<>(actionRequest, actionListener, span);

      return new SpanWithScope(span, TRACER.withSpan(span));
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.Enter final SpanWithScope scope, @Advice.Thrown final Throwable throwable) {
      if (throwable != null) {
        final Span span = scope.getSpan();
        DECORATE.onError(span, throwable);
        DECORATE.beforeFinish(span);
        span.end();
      }
      scope.closeScope();
    }
  }
}
