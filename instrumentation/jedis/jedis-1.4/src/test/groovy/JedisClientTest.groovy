/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import static io.opentelemetry.trace.Span.Kind.CLIENT

import io.opentelemetry.auto.test.AgentTestRunner
import io.opentelemetry.auto.test.utils.PortUtils
import io.opentelemetry.trace.attributes.SemanticAttributes
import redis.clients.jedis.Jedis
import redis.embedded.RedisServer
import spock.lang.Shared

class JedisClientTest extends AgentTestRunner {

  @Shared
  int port = PortUtils.randomOpenPort()

  @Shared
  RedisServer redisServer = RedisServer.builder()
  // bind to localhost to avoid firewall popup
    .setting("bind 127.0.0.1")
  // set max memory to avoid problems in CI
    .setting("maxmemory 128M")
    .port(port).build()
  @Shared
  Jedis jedis = new Jedis("localhost", port)

  def setupSpec() {
    println "Using redis: $redisServer.args"
    redisServer.start()
  }

  def cleanupSpec() {
    redisServer.stop()
//    jedis.close()  // not available in the early version we're using.
  }

  def setup() {
    jedis.flushAll()
    TEST_WRITER.clear()
  }

  def "set command"() {
    when:
    jedis.set("foo", "bar")

    then:
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          operationName "SET"
          spanKind CLIENT
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key()}" "redis"
            "${SemanticAttributes.DB_CONNECTION_STRING.key()}" "localhost:$port"
            "${SemanticAttributes.DB_STATEMENT.key()}" "SET"
            "${SemanticAttributes.NET_PEER_IP.key()}" "127.0.0.1"
            "${SemanticAttributes.NET_PEER_NAME.key()}" "localhost"
            "${SemanticAttributes.NET_PEER_PORT.key()}" port
          }
        }
      }
    }
  }

  def "get command"() {
    when:
    jedis.set("foo", "bar")
    def value = jedis.get("foo")

    then:
    value == "bar"

    assertTraces(2) {
      trace(0, 1) {
        span(0) {
          operationName "SET"
          spanKind CLIENT
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key()}" "redis"
            "${SemanticAttributes.DB_CONNECTION_STRING.key()}" "localhost:$port"
            "${SemanticAttributes.DB_STATEMENT.key()}" "SET"
            "${SemanticAttributes.NET_PEER_IP.key()}" "127.0.0.1"
            "${SemanticAttributes.NET_PEER_NAME.key()}" "localhost"
            "${SemanticAttributes.NET_PEER_PORT.key()}" port
          }
        }
      }
      trace(1, 1) {
        span(0) {
          operationName "GET"
          spanKind CLIENT
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key()}" "redis"
            "${SemanticAttributes.DB_CONNECTION_STRING.key()}" "localhost:$port"
            "${SemanticAttributes.DB_STATEMENT.key()}" "GET"
            "${SemanticAttributes.NET_PEER_IP.key()}" "127.0.0.1"
            "${SemanticAttributes.NET_PEER_NAME.key()}" "localhost"
            "${SemanticAttributes.NET_PEER_PORT.key()}" port
          }
        }
      }
    }
  }

  def "command with no arguments"() {
    when:
    jedis.set("foo", "bar")
    def value = jedis.randomKey()

    then:
    value == "foo"

    assertTraces(2) {
      trace(0, 1) {
        span(0) {
          operationName "SET"
          spanKind CLIENT
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key()}" "redis"
            "${SemanticAttributes.DB_CONNECTION_STRING.key()}" "localhost:$port"
            "${SemanticAttributes.DB_STATEMENT.key()}" "SET"
            "${SemanticAttributes.NET_PEER_IP.key()}" "127.0.0.1"
            "${SemanticAttributes.NET_PEER_NAME.key()}" "localhost"
            "${SemanticAttributes.NET_PEER_PORT.key()}" port
          }
        }
      }
      trace(1, 1) {
        span(0) {
          operationName "RANDOMKEY"
          spanKind CLIENT
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key()}" "redis"
            "${SemanticAttributes.DB_CONNECTION_STRING.key()}" "localhost:$port"
            "${SemanticAttributes.DB_STATEMENT.key()}" "RANDOMKEY"
            "${SemanticAttributes.NET_PEER_IP.key()}" "127.0.0.1"
            "${SemanticAttributes.NET_PEER_NAME.key()}" "localhost"
            "${SemanticAttributes.NET_PEER_PORT.key()}" port
          }
        }
      }
    }
  }
}
