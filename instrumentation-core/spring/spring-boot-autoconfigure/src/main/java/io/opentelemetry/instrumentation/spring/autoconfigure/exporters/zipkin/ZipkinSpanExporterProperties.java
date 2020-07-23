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

package io.opentelemetry.instrumentation.spring.autoconfigure.exporters.zipkin;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for {@link ZipkinSpanExporter}
 *
 * <p>Get Exporter Service Name {@link getServiceName()}
 *
 * <p>Get Exporter Endpoint
 */
@ConfigurationProperties(prefix = "opentelemetry.trace.exporter.zipkin")
public class ZipkinSpanExporterProperties {

  private boolean enabled = true;
  private String serviceName = "unknown";
  private String endpoint = "http://localhost:14250";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }
}
