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

package io.opentelemetry.instrumentation.spring.autoconfigure.exporters;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.exporters.zipkin.ZipkinSpanExporter;
import io.opentelemetry.instrumentation.spring.autoconfigure.TracerAutoConfiguration;
import io.opentelemetry.instrumentation.spring.autoconfigure.exporters.zipkin.ZipkinSpanExporterAutoConfiguration;
import io.opentelemetry.instrumentation.spring.autoconfigure.exporters.zipkin.ZipkinSpanExporterProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Spring Boot auto configuration test for {@link ZipkinSpanExporter}. */
class ZipkinSpanExporterAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  TracerAutoConfiguration.class, ZipkinSpanExporterAutoConfiguration.class));

  @Test
  @DisplayName("when exporters are ENABLED should initialize ZipkinSpanExporter bean")
  public void shouldInitializeZipkinSpanExporterBeanWhenExportersAreEnabled() {
    this.contextRunner
        .withPropertyValues("opentelemetry.trace.exporters.zipkin.enabled=true")
        .run(
            (context) -> {
              assertThat(context.getBean("otelZipkinSpanExporter", ZipkinSpanExporter.class))
                  .isNotNull();
            });
  }

  @Test
  @DisplayName(
      "when opentelemetry.trace.exporter.zipkin properties are set should initialize ZipkinSpanExporterProperties with property values")
  public void shouldInitializeZipkinSpanExporterBeanWithPropertyValues() {
    this.contextRunner
        .withPropertyValues(
            "opentelemetry.trace.exporter.zipkin.enabled=true",
            "opentelemetry.trace.exporter.zipkin.servicename=test",
            "opentelemetry.trace.exporter.zipkin.endpoint=http://localhost:8080/test")
        .run(
            (context) -> {
              ZipkinSpanExporterProperties zipkinSpanExporterProperties =
                  context.getBean(ZipkinSpanExporterProperties.class);
              assertThat(zipkinSpanExporterProperties.getServiceName()).isEqualTo("test");
              assertThat(zipkinSpanExporterProperties.getEndpoint())
                  .isEqualTo("http://localhost:8080/test");
            });
  }

  @Test
  @DisplayName("when exporters are DISABLED should NOT initialize ZipkinSpanExporter bean")
  public void shouldNotInitializeZipkinSpanExporterBeanWhenExportersAreDisabled() {
    this.contextRunner
        .withPropertyValues("opentelemetry.trace.exporter.zipkin.enabled=false")
        .run(
            (context) -> {
              assertThat(context.containsBean("otelZipkinSpanExporter")).isFalse();
            });
  }

  @Test
  @DisplayName("when zipkin enabled property is MISSING should initialize ZipkinSpanExporter bean")
  public void shouldInitializeZipkinSpanExporterBeanWhenZipkinEnabledPropertyIsMissing() {
    this.contextRunner.run(
        (context) -> {
          assertThat(context.getBean("otelZipkinSpanExporter", ZipkinSpanExporter.class))
              .isNotNull();
        });
  }
}
