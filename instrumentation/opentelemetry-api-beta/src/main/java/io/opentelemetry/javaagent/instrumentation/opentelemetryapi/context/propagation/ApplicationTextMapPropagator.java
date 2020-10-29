/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.opentelemetryapi.context.propagation;

import application.io.opentelemetry.context.Context;
import application.io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.javaagent.instrumentation.opentelemetryapi.context.AgentContextStorage;
import java.util.List;

class ApplicationTextMapPropagator implements TextMapPropagator {

  private final io.opentelemetry.context.propagation.TextMapPropagator agentTextMapPropagator;

  ApplicationTextMapPropagator(
      io.opentelemetry.context.propagation.TextMapPropagator agentTextMapPropagator) {
    this.agentTextMapPropagator = agentTextMapPropagator;
  }

  @Override
  public List<String> fields() {
    return agentTextMapPropagator.fields();
  }

  @Override
  public <C> Context extract(
      Context applicationContext, C carrier, TextMapPropagator.Getter<C> applicationGetter) {
    io.opentelemetry.context.Context agentContext =
        AgentContextStorage.getAgentContext(applicationContext);
    io.opentelemetry.context.Context agentUpdatedContext =
        agentTextMapPropagator.extract(agentContext, carrier, new AgentGetter<>(applicationGetter));
    if (agentUpdatedContext == agentContext) {
      return applicationContext;
    }
    return new AgentContextStorage.AgentContextWrapper(agentUpdatedContext, applicationContext);
  }

  @Override
  public <C> void inject(
      Context applicationContext, C carrier, TextMapPropagator.Setter<C> applicationSetter) {
    io.opentelemetry.context.Context agentContext =
        AgentContextStorage.getAgentContext(applicationContext);
    agentTextMapPropagator.inject(agentContext, carrier, new AgentSetter<>(applicationSetter));
  }

  private static class AgentGetter<C>
      implements io.opentelemetry.context.propagation.TextMapPropagator.Getter<C> {

    private final TextMapPropagator.Getter<C> applicationGetter;

    AgentGetter(TextMapPropagator.Getter<C> applicationGetter) {
      this.applicationGetter = applicationGetter;
    }

    @Override
    public String get(C carrier, String key) {
      return applicationGetter.get(carrier, key);
    }
  }

  private static class AgentSetter<C>
      implements io.opentelemetry.context.propagation.TextMapPropagator.Setter<C> {

    private final TextMapPropagator.Setter<C> applicationSetter;

    AgentSetter(Setter<C> applicationSetter) {
      this.applicationSetter = applicationSetter;
    }

    @Override
    public void set(C carrier, String key, String value) {
      applicationSetter.set(carrier, key, value);
    }
  }
}
