/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */


import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.instrumentation.rxjava2.AbstractRxJava2Test
import io.opentelemetry.instrumentation.rxjava2.TracingAssembly
import io.reactivex.Single
import io.reactivex.functions.Consumer

import java.util.concurrent.CountDownLatch

import static io.opentelemetry.instrumentation.test.utils.TraceUtils.basicSpan
import static io.opentelemetry.instrumentation.test.utils.TraceUtils.runUnderTrace

class RxJava2SubscriptionTest extends AbstractRxJava2Test {

  void runnerSetupSpec() {
    super.runnerSetupSpec()
    TracingAssembly.enable()
  }

  def "subscription test"() {
    when:
    CountDownLatch latch = new CountDownLatch(1)
    runUnderTrace("parent") {
      Single<Connection> connection = Single.create {
        it.onSuccess(new Connection())
      }
      connection.subscribe(new Consumer<Connection>() {
        @Override
        void accept(Connection t) {
          t.query()
          latch.countDown()
        }
      })
    }
    latch.await()

    then:
    assertTraces(1) {
      trace(0, 2) {
        basicSpan(it, 0, "parent")
        basicSpan(it, 1, "Connection.query", span(0))
      }
    }
  }

  static class Connection {
    static int query() {
      def span = GlobalOpenTelemetry.getTracer("test").spanBuilder("Connection.query").startSpan()
      span.end()
      return new Random().nextInt()
    }
  }
}
