/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import io.opentelemetry.instrumentation.test.InstrumentationSpecification

abstract class AbstractSpringCloudStreamRabbitTest extends InstrumentationSpecification implements WithRabbitProducerConsumerTrait {

  abstract Class<?> additionalContextClass()

  def setupSpec() {
    startRabbit(additionalContextClass())
  }

  def cleanupSpec() {
    stopRabbit()
  }

  def "should propagate context through RabbitMQ"() {
    when:
    producerContext.getBean("producer", Runnable).run()

    then:
    assertTraces(1) {
      trace(0, 4) {
        span(0) {
          name "producer"
        }
        span(1) {
          name "testProducer.output"
          childOf span(0)
        }
        span(2) {
          name "testConsumer.input"
          childOf span(1)
        }
        span(3) {
          name "consumer"
          childOf span(2)
        }
      }
    }
  }
}
