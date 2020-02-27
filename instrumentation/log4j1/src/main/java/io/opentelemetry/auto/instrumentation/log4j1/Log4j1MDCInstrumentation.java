package io.opentelemetry.auto.instrumentation.log4j1;

import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;

import com.google.auto.service.AutoService;
import io.opentelemetry.auto.api.Config;
import io.opentelemetry.auto.api.GlobalTracer;
import io.opentelemetry.auto.tooling.Instrumenter;
import io.opentelemetry.auto.tooling.log.LogContextScopeListener;
import java.lang.reflect.Method;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(Instrumenter.class)
public class Log4j1MDCInstrumentation extends Instrumenter.Default {
  public static final String MDC_INSTRUMENTATION_NAME = "log4j1";

  public Log4j1MDCInstrumentation() {
    super(MDC_INSTRUMENTATION_NAME);
  }

  @Override
  protected boolean defaultEnabled() {
    return Config.get().isLogInjectionEnabled();
  }

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return named("org.apache.log4j.MDC");
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        isConstructor(), Log4j1MDCInstrumentation.class.getName() + "$MDCContextAdvice");
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {LogContextScopeListener.class.getName()};
  }

  public static class MDCContextAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void mdcClassInitialized(@Advice.This final Object instance) {
      if (instance == null) {
        return;
      }

      try {
        final Class<?> mdcClass = instance.getClass();
        final Method putMethod = mdcClass.getMethod("put", String.class, Object.class);
        final Method removeMethod = mdcClass.getMethod("remove", String.class);
        GlobalTracer.get().addScopeListener(new LogContextScopeListener(putMethod, removeMethod));
      } catch (final NoSuchMethodException e) {
        org.slf4j.LoggerFactory.getLogger(instance.getClass())
            .debug("Failed to add log4j ThreadContext span listener", e);
      }
    }
  }
}
