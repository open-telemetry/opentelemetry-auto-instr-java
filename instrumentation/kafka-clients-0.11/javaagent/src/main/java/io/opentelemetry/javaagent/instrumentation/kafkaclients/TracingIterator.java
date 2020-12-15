/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.kafkaclients;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import java.util.Iterator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TracingIterator implements Iterator<ConsumerRecord<?, ?>> {

  private static final Logger log = LoggerFactory.getLogger(TracingIterator.class);

  private final Iterator<ConsumerRecord<?, ?>> delegateIterator;
  private final KafkaConsumerTracer tracer;
  private final boolean propagationEnabled;

  /**
   * Note: this may potentially create problems if this iterator is used from different threads. But
   * at the moment we cannot do much about this.
   */
  private Span currentSpan;

  private Scope currentScope;

  public TracingIterator(
      Iterator<ConsumerRecord<?, ?>> delegateIterator, KafkaConsumerTracer tracer) {
    this.delegateIterator = delegateIterator;
    this.tracer = tracer;
    this.propagationEnabled = KafkaClientsConfiguration.isPropagationEnabled();
  }

  @Override
  public boolean hasNext() {
    closeScopeAndEndSpan();
    return delegateIterator.hasNext();
  }

  @Override
  public ConsumerRecord<?, ?> next() {
    // in case they didn't call hasNext()...
    closeScopeAndEndSpan();

    ConsumerRecord<?, ?> next = delegateIterator.next();

    try {
      if (next != null) {
        currentSpan = tracer.startSpan(next);
        currentScope = currentSpan.makeCurrent();
      }
    } catch (Exception e) {
      log.debug("Error during decoration", e);
    }
    return next;
  }

  private void closeScopeAndEndSpan() {
    if (currentScope != null) {
      currentScope.close();
      currentScope = null;
      tracer.end(currentSpan);
      currentSpan = null;
    }
  }

  @Override
  public void remove() {
    delegateIterator.remove();
  }
}
