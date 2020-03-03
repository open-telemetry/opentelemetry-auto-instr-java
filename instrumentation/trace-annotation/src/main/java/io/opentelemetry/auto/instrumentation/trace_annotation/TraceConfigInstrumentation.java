/*
 * Copyright 2020, OpenTelemetry Authors
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
package io.opentelemetry.auto.instrumentation.trace_annotation;

import static io.opentelemetry.auto.tooling.bytebuddy.matcher.AgentElementMatchers.safeHasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.named;

import com.google.auto.service.AutoService;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.opentelemetry.auto.config.Config;
import io.opentelemetry.auto.tooling.Instrumenter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * TraceConfig Instrumentation does not extend Default.
 *
 * <p>Instead it directly implements Instrumenter#instrument() and adds one default Instrumenter for
 * every configured class+method-list.
 *
 * <p>If this becomes a more common use case the building logic should be abstracted out into a
 * super class.
 */
@Slf4j
@AutoService(Instrumenter.class)
public class TraceConfigInstrumentation implements Instrumenter {

  static final String PACKAGE_CLASS_NAME_REGEX = "[\\w.\\$]+";
  private static final String METHOD_LIST_REGEX = "\\s*(?:\\w+\\s*,)*\\s*(?:\\w+\\s*,?)\\s*";
  private static final String CONFIG_FORMAT =
      "(?:\\s*"
          + PACKAGE_CLASS_NAME_REGEX
          + "\\["
          + METHOD_LIST_REGEX
          + "\\]\\s*;)*\\s*"
          + PACKAGE_CLASS_NAME_REGEX
          + "\\["
          + METHOD_LIST_REGEX
          + "\\]";

  private final Map<String, Set<String>> classMethodsToTrace;

  private boolean validateConfigString(final String configString) {
    for (final String segment : configString.split(";")) {
      if (!segment.trim().matches(CONFIG_FORMAT)) {
        return false;
      }
    }
    return true;
  }

  public TraceConfigInstrumentation() {
    final String configString = Config.get().getTraceMethods();
    if (configString == null || configString.trim().isEmpty()) {
      classMethodsToTrace = Collections.emptyMap();

    } else if (!validateConfigString(configString)) {
      log.warn(
          "Invalid trace method config '{}'. Must match 'package.Class$Name[method1,method2];*'.",
          configString);
      classMethodsToTrace = Collections.emptyMap();

    } else {
      final Map<String, Set<String>> toTrace = Maps.newHashMap();
      final String[] classMethods = configString.split(";", -1);
      for (final String classMethod : classMethods) {
        if (classMethod.trim().isEmpty()) {
          continue;
        }
        final String[] splitClassMethod = classMethod.split("\\[", -1);
        final String className = splitClassMethod[0];
        final String method = splitClassMethod[1].trim();
        final String methodNames = method.substring(0, method.length() - 1);
        final String[] splitMethodNames = methodNames.split(",", -1);
        final Set<String> trimmedMethodNames =
            Sets.newHashSetWithExpectedSize(splitMethodNames.length);
        for (final String methodName : splitMethodNames) {
          final String trimmedMethodName = methodName.trim();
          if (!trimmedMethodName.isEmpty()) {
            trimmedMethodNames.add(trimmedMethodName);
          }
        }
        if (!trimmedMethodNames.isEmpty()) {
          toTrace.put(className.trim(), trimmedMethodNames);
        }
      }
      classMethodsToTrace = Collections.unmodifiableMap(toTrace);
    }
  }

  @Override
  public AgentBuilder instrument(AgentBuilder agentBuilder) {
    if (classMethodsToTrace.isEmpty()) {
      return agentBuilder;
    }

    for (final Map.Entry<String, Set<String>> entry : classMethodsToTrace.entrySet()) {
      final TracerClassInstrumentation tracerConfigClass =
          new TracerClassInstrumentation(entry.getKey(), entry.getValue());
      agentBuilder = tracerConfigClass.instrument(agentBuilder);
    }
    return agentBuilder;
  }

  // Not Using AutoService to hook up this instrumentation
  public static class TracerClassInstrumentation extends Default {
    private final String className;
    private final Set<String> methodNames;

    /** No-arg constructor only used by muzzle and tests. */
    public TracerClassInstrumentation() {
      this("noop", Collections.singleton("noop"));
    }

    public TracerClassInstrumentation(final String className, final Set<String> methodNames) {
      super("trace", "trace-config");
      this.className = className;
      this.methodNames = methodNames;
    }

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return safeHasSuperType(named(className));
    }

    @Override
    public String[] helperClassNames() {
      return new String[] {
        "io.opentelemetry.auto.decorator.BaseDecorator", packageName + ".TraceDecorator",
      };
    }

    @Override
    public Map<ElementMatcher<? super MethodDescription>, String> transformers() {
      ElementMatcher.Junction<MethodDescription> methodMatchers = null;
      for (final String methodName : methodNames) {
        if (methodMatchers == null) {
          methodMatchers = named(methodName);
        } else {
          methodMatchers = methodMatchers.or(named(methodName));
        }
      }

      final Map<ElementMatcher<? super MethodDescription>, String> transformers = new HashMap<>();
      transformers.put(methodMatchers, packageName + ".TraceAdvice");
      return transformers;
    }
  }
}
