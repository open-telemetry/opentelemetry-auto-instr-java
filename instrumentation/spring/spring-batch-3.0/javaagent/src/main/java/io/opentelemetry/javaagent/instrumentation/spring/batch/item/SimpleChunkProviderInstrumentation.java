/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spring.batch.item;

import static io.opentelemetry.javaagent.instrumentation.spring.batch.SpringBatchInstrumentationConfig.isItemLevelTracingEnabled;
import static io.opentelemetry.javaagent.instrumentation.spring.batch.item.ItemTracer.tracer;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isProtected;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

// item read instrumentation *cannot* use ItemReadListener: sometimes afterRead() is not called
// after beforeRead(), using listener here would cause unfinished spans/scopes
public class SimpleChunkProviderInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return named("org.springframework.batch.core.step.item.SimpleChunkProvider");
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        isProtected().and(named("doRead")).and(takesArguments(0)),
        this.getClass().getName() + "$ReadAdvice");
  }

  public static class ReadAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.Local("otelContext") Context context, @Advice.Local("otelScope") Scope scope) {
      if (!isItemLevelTracingEnabled()) {
        return;
      }
      context = tracer().startReadSpan();
      if (context != null) {
        scope = context.makeCurrent();
      }
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void onExit(
        @Advice.Thrown Throwable thrown,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      if (scope != null) {
        scope.close();
        if (thrown == null) {
          tracer().end(context);
        } else {
          tracer().endExceptionally(context, thrown);
        }
      }
    }
  }
}
