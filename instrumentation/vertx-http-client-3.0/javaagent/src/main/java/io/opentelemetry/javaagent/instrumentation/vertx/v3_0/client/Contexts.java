/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.vertx.v3_0.client;

import io.opentelemetry.context.Context;

public class Contexts {
  public final Context parentContext;
  public final Context context;

  public Contexts(Context parentContext, Context context) {
    this.parentContext = parentContext;
    this.context = context;
  }
}
