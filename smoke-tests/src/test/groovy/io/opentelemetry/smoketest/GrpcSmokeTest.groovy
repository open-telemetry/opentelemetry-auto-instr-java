/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.smoketest

import static java.util.stream.Collectors.toSet

import io.grpc.ManagedChannelBuilder
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc
import java.util.jar.Attributes
import java.util.jar.JarFile
import spock.lang.Unroll

class GrpcSmokeTest extends SmokeTest {

  protected String getTargetImage(int jdk, String serverVersion) {
    "ghcr.io/open-telemetry/java-test-containers:smoke-grpc-jdk$jdk-20201204.400701585"
  }

  @Unroll
  def "grpc smoke test on JDK #jdk"(int jdk) {
    setup:
    startTarget(jdk)

    def channel = ManagedChannelBuilder.forAddress("localhost", target.getMappedPort(8080))
      .usePlaintext()
      .build()
    def stub = TraceServiceGrpc.newBlockingStub(channel)

    def currentAgentVersion = new JarFile(agentPath).getManifest().getMainAttributes().get(Attributes.Name.IMPLEMENTATION_VERSION)

    when:
    stub.export(ExportTraceServiceRequest.getDefaultInstance())
    Collection<ExportTraceServiceRequest> traces = waitForTraces()

    then:
    countSpansByName(traces, 'opentelemetry.proto.collector.trace.v1.TraceService/Export') == 1
    countSpansByName(traces, 'TestService.withSpan') == 1

    [currentAgentVersion] as Set == findResourceAttribute(traces, "telemetry.auto.version")
      .map { it.stringValue }
      .collect(toSet())

    cleanup:
    stopTarget()

    where:
    jdk << [8, 11, 15]
  }
}
