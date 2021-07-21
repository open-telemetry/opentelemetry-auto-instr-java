/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import io.opentelemetry.config.Configs;
import io.opentelemetry.config.TestConfig;
import io.opentelemetry.containers.CollectorContainer;
import io.opentelemetry.containers.K6Container;
import io.opentelemetry.containers.PetClinicRestContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

public class OverheadTests {

  private static final Network NETWORK = Network.newNetwork();
  private static GenericContainer<?> collector;
  private final NamingConvention containerNamingConvention = new NamingConvention("/results");
  private final NamingConvention localNamingConvention = new NamingConvention();

  @BeforeAll
  static void setUp() {
    collector = CollectorContainer.build(NETWORK);
    collector.start();
  }

  @AfterAll
  static void tearDown() {
    collector.close();
  }

  @Test
  @Disabled
  void results() {
    Map<String, AppPerfResults> results = new ResultsCollector(localNamingConvention)
        .collect(Configs.RELEASE.config);
    new ConsoleResultsPersister().write(results);
  }

  @TestFactory
  Stream<DynamicTest> runAllTestConfigurations() {
    return Configs.all().map(config ->
        dynamicTest(config.getName(), () -> runTestConfig(config))
    );
  }

  void runTestConfig(TestConfig config) {
    config.getAgents().forEach(agent -> {
      try {
        runAppOnce(config, agent);
      } catch (Exception e) {
        fail("Unhandled exception in " + config.getName(), e);
      }
    });
    Map<String, AppPerfResults> results = new ResultsCollector(localNamingConvention).collect(config);
    new ConsoleResultsPersister().write(results);
  }

  void runAppOnce(TestConfig config, String agent) throws Exception {
    GenericContainer<?> petclinic = new PetClinicRestContainer(NETWORK, collector, agent, containerNamingConvention).build();
    petclinic.start();

    GenericContainer<?> k6 = new K6Container(NETWORK, agent, config, containerNamingConvention).build();
    k6.start();

    // This is required to get a graceful exit of the VM before testcontainers kills it forcibly.
    // Without it, our jfr file will be empty.
    petclinic.execInContainer("kill", "1");
    while (petclinic.isRunning()) {
      TimeUnit.MILLISECONDS.sleep(500);
    }

    //TODO: Parse and aggregate the test results.
  }
}
