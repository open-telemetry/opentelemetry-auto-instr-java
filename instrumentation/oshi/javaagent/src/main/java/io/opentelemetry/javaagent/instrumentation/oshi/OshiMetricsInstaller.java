/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.oshi;

import com.google.auto.service.AutoService;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.spi.ComponentInstaller;
import java.lang.reflect.Method;
import java.util.Collections;

/**
 * {@link ComponentInstaller} to enable oshi metrics during agent startup if oshi is present on the
 * system classpath.
 */
@AutoService(ComponentInstaller.class)
public class OshiMetricsInstaller implements ComponentInstaller {
  @Override
  public void afterByteBuddyAgent(Config config) {
    if (config.isInstrumentationEnabled(
        Collections.singleton("oshi"), /* defaultEnabled= */ true)) {
      try {
        // Call oshi.SystemInfo.getCurrentPlatformEnum() to activate SystemMetrics.
        // Oshi instrumentation will intercept this call and enable SystemMetrics.
        Class<?> oshiSystemInfoClass =
            ClassLoader.getSystemClassLoader().loadClass("oshi.SystemInfo");
        Method getCurrentPlatformEnumMethod =
            oshiSystemInfoClass.getMethod("getCurrentPlatformEnum");
        getCurrentPlatformEnumMethod.invoke(null);
      } catch (Throwable ex) {
        // OK
      }
    }
  }
}
