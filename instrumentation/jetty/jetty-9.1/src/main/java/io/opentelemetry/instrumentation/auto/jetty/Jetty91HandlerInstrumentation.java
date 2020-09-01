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

package io.opentelemetry.instrumentation.auto.jetty;

import static io.opentelemetry.javaagent.tooling.ClassLoaderMatcher.hasClassesNamed;
import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.implementsInterface;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.tooling.Instrumenter;
import java.util.Map;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(Instrumenter.class)
public final class Jetty91HandlerInstrumentation extends Instrumenter.Default {

  public Jetty91HandlerInstrumentation() {
    super("jetty", "jetty-9.1");
  }

  @Override
  public ElementMatcher<ClassLoader> classLoaderMatcher() {
    // Jetty 9.1 uses servlet-api 3.1
    // WriteListener was added in servlet-api 3.1, 3.0 does not have it
    return hasClassesNamed("org.eclipse.jetty.server.Handler", "javax.servlet.WriteListener");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return not(named("org.eclipse.jetty.server.handler.HandlerWrapper"))
        .and(implementsInterface(named("org.eclipse.jetty.server.Handler")));
  }

  @Override
  public String[] helperClassNames() {
    // order matters here because subclasses (e.g. JettyHttpServerTracer) need to be injected into
    // the class loader after their super classes (e.g. Servlet3HttpServerTracer)
    String commonPackageName = "io.opentelemetry.instrumentation.auto.servlet.v3";
    String servlet31PackageName = "io.opentelemetry.instrumentation.auto.servlet.v3_1";
    return new String[] {
      "io.opentelemetry.instrumentation.servlet.HttpServletRequestGetter",
      "io.opentelemetry.instrumentation.servlet.ServletHttpServerTracer",
      commonPackageName + ".CountingHttpServletRequest",
      commonPackageName + ".AbstractCountingHttpServletResponse",
      commonPackageName + ".AbstractCountingHttpServletResponse$CountingPrintWriter",
      commonPackageName + ".Servlet3HttpServerTracer",
      commonPackageName + ".TagSettingAsyncListener",
      servlet31PackageName + ".Servlet31CountingHttpServletResponse",
      servlet31PackageName + ".Servlet31CountingHttpServletResponse$CountingServletOutputStream",
      packageName + ".Jetty91HttpServerTracer",
    };
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        named("handle")
            .and(takesArgument(0, named("java.lang.String")))
            .and(takesArgument(1, named("org.eclipse.jetty.server.Request")))
            .and(takesArgument(2, named("javax.servlet.http.HttpServletRequest")))
            .and(takesArgument(3, named("javax.servlet.http.HttpServletResponse")))
            .and(isPublic()),
        packageName + ".Jetty91HandlerAdvice");
  }
}
