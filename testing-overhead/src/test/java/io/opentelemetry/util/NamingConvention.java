/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class NamingConvention {

  private final String dir;

  public NamingConvention(){
    this(".");
  }

  public NamingConvention(String dir) {this.dir = dir;}

  public Path k6Results(String agent){
    return Paths.get(dir, "k6_out_" + agent + ".json");
  }

  public Path jfrFile(String agentName) {
    return Paths.get(dir, "petclinic-" + agentName + ".jfr");
  }

  public Path startupDurationFile(String agentName) { return Paths.get(dir, "startup-time-" + agentName + ".txt"); }
}
