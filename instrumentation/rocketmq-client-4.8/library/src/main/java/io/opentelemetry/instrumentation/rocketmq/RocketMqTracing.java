/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rocketmq;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.config.Config;
import org.apache.rocketmq.client.hook.ConsumeMessageHook;
import org.apache.rocketmq.client.hook.SendMessageHook;

/** Entrypoint for tracing RocketMq producers or consumers. */
public final class RocketMqTracing {

  /** Returns a new {@link RocketMqTracing} configured with the given {@link OpenTelemetry}. */
  public static RocketMqTracing create(OpenTelemetry openTelemetry) {
    return newBuilder(openTelemetry)
        .setPropagationEnabled(
            Config.get()
                .getBooleanProperty("otel.instrumentation.rocketmq-client.propagation", true))
        .setCaptureExperimentalSpanAttributes(
            Config.get()
                .getBooleanProperty(
                    "otel.instrumentation.rocketmq-client.experimental-span-attributes", true))
        .build();
  }

  /**
   * Returns a new {@link RocketMqTracingBuilder} configured with the given {@link OpenTelemetry}.
   */
  public static RocketMqTracingBuilder newBuilder(OpenTelemetry openTelemetry) {
    return new RocketMqTracingBuilder(openTelemetry);
  }

  private final boolean captureExperimentalSpanAttributes;
  private final boolean propagationEnabled;

  private final RocketMqConsumerTracer rocketMqConsumerTracer;
  private final RocketMqProducerTracer rocketMqProducerTracer;

  RocketMqTracing(
      OpenTelemetry openTelemetry,
      boolean captureExperimentalSpanAttributes,
      boolean propagationEnabled) {
    this.captureExperimentalSpanAttributes = captureExperimentalSpanAttributes;
    this.propagationEnabled = propagationEnabled;
    rocketMqConsumerTracer =
        new RocketMqConsumerTracer(
            openTelemetry, captureExperimentalSpanAttributes, propagationEnabled);
    rocketMqProducerTracer =
        new RocketMqProducerTracer(openTelemetry, captureExperimentalSpanAttributes);
  }

  /**
   * Returns a new {@link ConsumeMessageHook} for use with methods like {@link
   * org.apache.rocketmq.client.impl.consumer.DefaultMQPullConsumerImpl#registerConsumeMessageHook(ConsumeMessageHook)}.
   */
  public ConsumeMessageHook newTracingConsumeMessageHook() {
    return new TracingConsumeMessageHookImpl(rocketMqConsumerTracer);
  }

  /**
   * Returns a new {@link SendMessageHook} for use with methods like {@link
   * org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl#registerSendMessageHook(SendMessageHook)}.
   */
  public SendMessageHook newTracingSendMessageHook() {
    return new TracingSendMessageHookImpl(rocketMqProducerTracer, propagationEnabled);
  }
}