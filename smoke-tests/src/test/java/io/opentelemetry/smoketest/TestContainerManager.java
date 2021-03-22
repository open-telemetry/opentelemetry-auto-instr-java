/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.smoketest;

import java.util.Map;
import java.util.function.Consumer;
import org.testcontainers.containers.output.OutputFrame;

public interface TestContainerManager {
  void startEnvironment();

  void stopEnvironment();

  int getBackendMappedPort();

  int getTargetMappedPort(int originalPort);

  Consumer<OutputFrame> startTarget(
      String targetImageName,
      String agentPath,
      Map<String, String> extraEnv,
      TargetWaitStrategy waitStrategy);

  void stopTarget();
}
