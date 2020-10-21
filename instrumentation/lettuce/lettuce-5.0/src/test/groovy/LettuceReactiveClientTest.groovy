/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import static io.opentelemetry.instrumentation.test.utils.TraceUtils.runUnderTrace
import static io.opentelemetry.trace.Span.Kind.CLIENT

import io.lettuce.core.ClientOptions
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulConnection
import io.lettuce.core.api.reactive.RedisReactiveCommands
import io.lettuce.core.api.sync.RedisCommands
import io.opentelemetry.instrumentation.test.AgentTestRunner
import io.opentelemetry.instrumentation.test.utils.PortUtils
import io.opentelemetry.trace.attributes.SemanticAttributes
import java.util.function.Consumer
import reactor.core.scheduler.Schedulers
import redis.embedded.RedisServer
import spock.lang.Shared
import spock.util.concurrent.AsyncConditions

class LettuceReactiveClientTest extends AgentTestRunner {
  public static final String PEER_HOST = "localhost"
  public static final String PEER_IP = "127.0.0.1"
  public static final int DB_INDEX = 0
  // Disable autoreconnect so we do not get stray traces popping up on server shutdown
  public static final ClientOptions CLIENT_OPTIONS = ClientOptions.builder().autoReconnect(false).build()

  @Shared
  String embeddedDbUri

  @Shared
  RedisServer redisServer

  RedisClient redisClient
  StatefulConnection connection
  RedisReactiveCommands<String, ?> reactiveCommands
  RedisCommands<String, ?> syncCommands

  def setupSpec() {
    int port = PortUtils.randomOpenPort()
    String dbAddr = PEER_HOST + ":" + port + "/" + DB_INDEX
    embeddedDbUri = "redis://" + dbAddr

    redisServer = RedisServer.builder()
    // bind to localhost to avoid firewall popup
      .setting("bind " + PEER_HOST)
    // set max memory to avoid problems in CI
      .setting("maxmemory 128M")
      .port(port).build()
  }

  def setup() {
    redisClient = RedisClient.create(embeddedDbUri)

    println "Using redis: $redisServer.args"
    redisServer.start()
    redisClient.setOptions(CLIENT_OPTIONS)

    connection = redisClient.connect()
    reactiveCommands = connection.reactive()
    syncCommands = connection.sync()

    syncCommands.set("TESTKEY", "TESTVAL")

    // 1 set + 1 connect trace
    TEST_WRITER.waitForTraces(2)
    TEST_WRITER.clear()
  }

  def cleanup() {
    connection.close()
    redisClient.shutdown()
    redisServer.stop()
  }

  def "set command with subscribe on a defined consumer"() {
    setup:
    def conds = new AsyncConditions()
    Consumer<String> consumer = new Consumer<String>() {
      @Override
      void accept(String res) {
        conds.evaluate {
          assert res == "OK"
        }
      }
    }

    when:
    reactiveCommands.set("TESTSETKEY", "TESTSETVAL").subscribe(consumer)

    then:
    conds.await()
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "SET TESTSETKEY ?"
          kind CLIENT
          errored false
          attributes {
            "$SemanticAttributes.DB_SYSTEM.key" "redis"
            "$SemanticAttributes.DB_STATEMENT.key" "SET TESTSETKEY ?"
          }
        }
      }
    }
  }

  def "get command with lambda function"() {
    setup:
    def conds = new AsyncConditions()

    when:
    reactiveCommands.get("TESTKEY").subscribe { res -> conds.evaluate { assert res == "TESTVAL" } }

    then:
    conds.await()
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "GET TESTKEY"
          kind CLIENT
          errored false
          attributes {
            "$SemanticAttributes.DB_SYSTEM.key" "redis"
            "$SemanticAttributes.DB_STATEMENT.key" "GET TESTKEY"
          }
        }
      }
    }
  }

  // to make sure instrumentation's chained completion stages won't interfere with user's, while still
  // recording metrics
  def "get non existent key command"() {
    setup:
    def conds = new AsyncConditions()
    final defaultVal = "NOT THIS VALUE"

    when:
    reactiveCommands.get("NON_EXISTENT_KEY").defaultIfEmpty(defaultVal).subscribe {
      res ->
        conds.evaluate {
          assert res == defaultVal
        }
    }

    then:
    conds.await()
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "GET NON_EXISTENT_KEY"
          kind CLIENT
          errored false
          attributes {
            "$SemanticAttributes.DB_SYSTEM.key" "redis"
            "$SemanticAttributes.DB_STATEMENT.key" "GET NON_EXISTENT_KEY"
          }
        }
      }
    }

  }

  def "command with no arguments"() {
    setup:
    def conds = new AsyncConditions()

    when:
    reactiveCommands.randomkey().subscribe {
      res ->
        conds.evaluate {
          assert res == "TESTKEY"
        }
    }

    then:
    conds.await()
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "RANDOMKEY"
          kind CLIENT
          errored false
          attributes {
            "$SemanticAttributes.DB_SYSTEM.key" "redis"
            "$SemanticAttributes.DB_STATEMENT.key" "RANDOMKEY"
          }
        }
      }
    }
  }

  def "command flux publisher "() {
    setup:
    reactiveCommands.command().subscribe()

    expect:
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "COMMAND"
          kind CLIENT
          errored false
          attributes {
            "$SemanticAttributes.DB_SYSTEM.key" "redis"
            "$SemanticAttributes.DB_STATEMENT.key" "COMMAND"
            "db.command.results.count" 157
          }
        }
      }
    }
  }

  def "command cancel after 2 on flux publisher "() {
    setup:
    reactiveCommands.command().take(2).subscribe()

    expect:
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "COMMAND"
          kind CLIENT
          errored false
          attributes {
            "$SemanticAttributes.DB_SYSTEM.key" "redis"
            "$SemanticAttributes.DB_STATEMENT.key" "COMMAND"
            "db.command.cancelled" true
            "db.command.results.count" 2
          }
        }
      }
    }
  }

  def "non reactive command should not produce span"() {
    setup:
    String res = null

    when:
    res = reactiveCommands.digest(null)

    then:
    res != null
    TEST_WRITER.traces.size() == 0
  }

  def "debug segfault command (returns mono void) with no argument should produce span"() {
    setup:
    reactiveCommands.debugSegfault().subscribe()

    expect:
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "DEBUG SEGFAULT"
          kind CLIENT
          errored false
          attributes {
            "$SemanticAttributes.DB_SYSTEM.key" "redis"
            "$SemanticAttributes.DB_STATEMENT.key" "DEBUG SEGFAULT"
          }
        }
      }
    }
  }

  def "shutdown command (returns void) with argument should produce span"() {
    setup:
    reactiveCommands.shutdown(false).subscribe()

    expect:
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "SHUTDOWN NOSAVE"
          kind CLIENT
          errored false
          attributes {
            "$SemanticAttributes.DB_SYSTEM.key" "redis"
            "$SemanticAttributes.DB_STATEMENT.key" "SHUTDOWN NOSAVE"
          }
        }
      }
    }
  }

  def "blocking subscriber"() {
    when:
    runUnderTrace("test-parent") {
      reactiveCommands.set("a", "1")
        .then(reactiveCommands.get("a"))
        .block()
    }

    then:
    assertTraces(1) {
      trace(0, 3) {
        span(0) {
          name "test-parent"
          errored false
          attributes {
          }
        }
        span(1) {
          name "SET a ?"
          kind CLIENT
          errored false
          childOf span(0)
          attributes {
            "$SemanticAttributes.DB_SYSTEM.key" "redis"
            "$SemanticAttributes.DB_STATEMENT.key" "SET a ?"
          }
        }
        span(2) {
          name "GET a"
          kind CLIENT
          errored false
          childOf span(0)
          attributes {
            "$SemanticAttributes.DB_SYSTEM.key" "redis"
            "$SemanticAttributes.DB_STATEMENT.key" "GET a"
          }
        }
      }
    }
  }

  def "async subscriber"() {
    when:
    runUnderTrace("test-parent") {
      reactiveCommands.set("a", "1")
        .then(reactiveCommands.get("a"))
        .subscribe()
    }

    then:
    assertTraces(1) {
      trace(0, 3) {
        span(0) {
          name "test-parent"
          errored false
          attributes {
          }
        }
        span(1) {
          name "SET a ?"
          kind CLIENT
          errored false
          childOf span(0)
          attributes {
            "$SemanticAttributes.DB_SYSTEM.key" "redis"
            "$SemanticAttributes.DB_STATEMENT.key" "SET a ?"
          }
        }
        span(2) {
          name "GET a"
          kind CLIENT
          errored false
          childOf span(0)
          attributes {
            "$SemanticAttributes.DB_SYSTEM.key" "redis"
            "$SemanticAttributes.DB_STATEMENT.key" "GET a"
          }
        }
      }
    }
  }

  def "async subscriber with specific thread pool"() {
    when:
    runUnderTrace("test-parent") {
      reactiveCommands.set("a", "1")
        .then(reactiveCommands.get("a"))
        .subscribeOn(Schedulers.elastic())
        .subscribe()
    }

    then:
    assertTraces(1) {
      trace(0, 3) {
        span(0) {
          name "test-parent"
          errored false
          attributes {
          }
        }
        span(1) {
          name "SET a ?"
          kind CLIENT
          errored false
          childOf span(0)
          attributes {
            "$SemanticAttributes.DB_SYSTEM.key" "redis"
            "$SemanticAttributes.DB_STATEMENT.key" "SET a ?"
          }
        }
        span(2) {
          name "GET a"
          kind CLIENT
          errored false
          childOf span(0)
          attributes {
            "$SemanticAttributes.DB_SYSTEM.key" "redis"
            "$SemanticAttributes.DB_STATEMENT.key" "GET a"
          }
        }
      }
    }
  }
}
