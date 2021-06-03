/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.otelannotations;

import static io.opentelemetry.javaagent.instrumentation.otelannotations.WithSpanTracer.tracer;
import static net.bytebuddy.matcher.ElementMatchers.declaresMethod;
import static net.bytebuddy.matcher.ElementMatchers.hasParameters;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.whereAny;

import application.io.opentelemetry.extension.annotations.WithSpan;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge;
import io.opentelemetry.javaagent.tooling.config.MethodsConfigurationParser;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

public class WithSpanInstrumentation implements TypeInstrumentation {

  private static final String TRACE_ANNOTATED_METHODS_EXCLUDE_CONFIG =
      "otel.instrumentation.opentelemetry-annotations.exclude-methods";

  private final ElementMatcher.Junction<AnnotationSource> annotatedMethodMatcher;
  private final ElementMatcher.Junction<MethodDescription> annotatedParametersMatcher;
  // this matcher matches all methods that should be excluded from transformation
  private final ElementMatcher.Junction<MethodDescription> excludedMethodsMatcher;

  WithSpanInstrumentation() {
    annotatedMethodMatcher =
        isAnnotatedWith(named("application.io.opentelemetry.extension.annotations.WithSpan"));
    annotatedParametersMatcher =
        hasParameters(
            whereAny(
                isAnnotatedWith(
                    named(
                        "application.io.opentelemetry.javaagent.instrumentation.otelannotations.SpanAttribute"))));
    excludedMethodsMatcher = configureExcludedMethods();
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return declaresMethod(annotatedMethodMatcher);
  }

  @Override
  public void transform(TypeTransformer transformer) {
    ElementMatcher.Junction<MethodDescription> tracedMethods =
        annotatedMethodMatcher.and(not(excludedMethodsMatcher));

    ElementMatcher.Junction<MethodDescription> tracedMethodsWithParameters =
        tracedMethods.and(annotatedParametersMatcher);
    ElementMatcher.Junction<MethodDescription> tracedMethodsWithoutParameters =
        tracedMethods.and(not(annotatedParametersMatcher));

    transformer.applyAdviceToMethod(
        tracedMethods, WithSpanInstrumentation.class.getName() + "$WithSpanAttributesAdvice");

    // TODO: To avoid copying/boxing parameters only use WithSpanAttributesAdvice
    //      if method has any parameters annotated with @SpanAttribute
    /*
    transformer.applyAdviceToMethod(tracedMethodsWithoutParameters,
        WithSpanInstrumentation.class.getName() + "$WithSpanAdvice");

    transformer.applyAdviceToMethod(tracedMethodsWithParameters,
        WithSpanInstrumentation.class.getName() + "$WithSpanAttributesAdvice");
     */
  }

  /*
  Returns a matcher for all methods that should be excluded from auto-instrumentation by
  annotation-based advices.
  */
  static ElementMatcher.Junction<MethodDescription> configureExcludedMethods() {
    ElementMatcher.Junction<MethodDescription> result = none();

    Map<String, Set<String>> excludedMethods =
        MethodsConfigurationParser.parse(
            Config.get().getProperty(TRACE_ANNOTATED_METHODS_EXCLUDE_CONFIG));
    for (Map.Entry<String, Set<String>> entry : excludedMethods.entrySet()) {
      String className = entry.getKey();
      ElementMatcher.Junction<ByteCodeElement> classMather =
          isDeclaredBy(ElementMatchers.named(className));

      ElementMatcher.Junction<MethodDescription> excludedMethodsMatcher = none();
      for (String methodName : entry.getValue()) {
        excludedMethodsMatcher = excludedMethodsMatcher.or(ElementMatchers.named(methodName));
      }

      result = result.or(classMather.and(excludedMethodsMatcher));
    }

    return result;
  }

  public static class WithSpanAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.Origin Method method,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      WithSpan applicationAnnotation = method.getAnnotation(WithSpan.class);

      SpanKind kind = tracer().extractSpanKind(applicationAnnotation);
      Context current = Java8BytecodeBridge.currentContext();

      // don't create a nested span if you're not supposed to.
      if (tracer().shouldStartSpan(current, kind)) {
        context = tracer().startSpan(current, applicationAnnotation, method, kind, null);
        scope = context.makeCurrent();
      }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.Origin Method method,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope,
        @Advice.Return(typing = Assigner.Typing.DYNAMIC, readOnly = false) Object returnValue,
        @Advice.Thrown Throwable throwable) {
      if (scope == null) {
        return;
      }
      scope.close();

      if (throwable != null) {
        tracer().endExceptionally(context, throwable);
      } else {
        returnValue = tracer().end(context, method.getReturnType(), returnValue);
      }
    }
  }

  public static class WithSpanAttributesAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.Origin Method method,
        @Advice.AllArguments(readOnly = true, typing = Assigner.Typing.DYNAMIC) Object[] args,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      WithSpan applicationAnnotation = method.getAnnotation(WithSpan.class);

      SpanKind kind = tracer().extractSpanKind(applicationAnnotation);
      Context current = Java8BytecodeBridge.currentContext();

      // don't create a nested span if you're not supposed to.
      if (tracer().shouldStartSpan(current, kind)) {
        context = tracer().startSpan(current, applicationAnnotation, method, kind, args);
        scope = context.makeCurrent();
      }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.Origin Method method,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope,
        @Advice.Return(typing = Assigner.Typing.DYNAMIC, readOnly = false) Object returnValue,
        @Advice.Thrown Throwable throwable) {
      if (scope == null) {
        return;
      }
      scope.close();

      if (throwable != null) {
        tracer().endExceptionally(context, throwable);
      } else {
        returnValue = tracer().end(context, method.getReturnType(), returnValue);
      }
    }
  }
}
