/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import io.opentelemetry.auto.test.AgentTestRunner
import java.util.concurrent.TimeUnit
import org.springframework.context.annotation.AnnotationConfigApplicationContext

class SpringSchedulingTest extends AgentTestRunner {

  def "schedule trigger test according to cron expression"() {
    setup:
    def context = new AnnotationConfigApplicationContext(TriggerTaskConfig)
    def task = context.getBean(TriggerTask)

    task.blockUntilExecute()

    expect:
    assert task != null
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          operationName "TriggerTask.run"
          parent()
          errored false
          attributes {
          }
        }
      }
    }
  }

  def "schedule interval test"() {
    setup:
    def context = new AnnotationConfigApplicationContext(IntervalTaskConfig)
    def task = context.getBean(IntervalTask)

    task.blockUntilExecute()

    expect:
    assert task != null
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          operationName "IntervalTask.run"
          parent()
          errored false
          attributes {
          }
        }
      }
    }

  }

  def "schedule lambda test"() {
    setup:
    def context = new AnnotationConfigApplicationContext(LambdaTaskConfig)
    def configurer = context.getBean(LambdaTaskConfigurer)

    configurer.singleUseLatch.await(2000, TimeUnit.MILLISECONDS)

    expect:
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          operationNameContains "LambdaTaskConfigurer\$\$Lambda\$"
          parent()
          errored false
          attributes {
          }
        }
      }
    }

    cleanup:
    context.close()
  }
}
