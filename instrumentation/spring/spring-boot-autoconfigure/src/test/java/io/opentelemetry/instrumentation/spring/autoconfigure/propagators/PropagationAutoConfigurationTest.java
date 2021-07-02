/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.propagators;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.instrumentation.spring.autoconfigure.OpenTelemetryAutoConfiguration;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PropagationAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(OpenTelemetryAutoConfiguration.class));

  @AfterEach
  void tearDown() {
    GlobalOpenTelemetry.resetForTest();
  }

  @Test
  @DisplayName("when propagation is ENABLED should initialize PropagationAutoConfiguration bean")
  void shouldBeConfigured() {

    this.contextRunner
        .withPropertyValues("otel.propagation.enabled=true")
        .run(
            context ->
                assertThat(context.getBean("contextPropagators", ContextPropagators.class))
                    .isNotNull());
  }

  @Test
  @DisplayName(
      "when propagation is DISABLED should NOT initialize PropagationAutoConfiguration bean")
  void shouldNotBeConfigured() {

    this.contextRunner
        .withPropertyValues("otel.propagation.enabled=false")
        .run(context -> assertThat(context.containsBean("contextPropagators")).isFalse());
  }

  @Test
  @DisplayName(
      "when propagation enabled property is MISSING should initialize PropagationAutoConfiguration bean")
  void noProperty() {
    this.contextRunner.run(
        context ->
            assertThat(context.getBean("contextPropagators", ContextPropagators.class))
                .isNotNull());
  }

  @Test
  @DisplayName("when no propagators are defined should contain default propagators")
  void shouldContainDefaults() {

    this.contextRunner.run(
        context ->
            assertThat(
                    context
                        .getBean("compositeTextMapPropagator", CompositeTextMapPropagator.class)
                        .fields())
                .contains("traceparent", "baggage"));
  }

  @Test
  @DisplayName("when propagation is set to b3 should contain only b3 propagator")
  void shouldContainB3() {
    this.contextRunner
        .withPropertyValues("otel.propagation.type=B3")
        .run(
            context -> {
              CompositeTextMapPropagator compositePropagator =
                  context.getBean("compositeTextMapPropagator", CompositeTextMapPropagator.class);

              assertThat(compositePropagator.fields()).contains("b3");
              assertThat(compositePropagator.fields())
                  .doesNotContainAnyElementsOf(Arrays.asList("baggage", "traceparent"));
            });
  }
}
