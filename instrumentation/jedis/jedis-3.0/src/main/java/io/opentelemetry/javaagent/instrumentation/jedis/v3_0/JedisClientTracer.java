/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jedis.v3_0;

import io.opentelemetry.instrumentation.api.tracer.DatabaseClientTracer;
import io.opentelemetry.javaagent.instrumentation.api.db.DbSystem;
import io.opentelemetry.javaagent.instrumentation.api.db.RedisCommandNormalizer;
import io.opentelemetry.javaagent.instrumentation.jedis.v3_0.JedisClientTracer.CommandWithArgs;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.commands.ProtocolCommand;

public class JedisClientTracer extends DatabaseClientTracer<Connection, CommandWithArgs> {
  public static final JedisClientTracer TRACER = new JedisClientTracer();

  @Override
  protected String normalizeQuery(CommandWithArgs command) {
    return RedisCommandNormalizer.normalize(command.getStringCommand(), command.getStringArgs());
  }

  @Override
  protected String dbSystem(Connection connection) {
    return DbSystem.REDIS;
  }

  @Override
  protected String dbConnectionString(Connection connection) {
    return connection.getHost() + ":" + connection.getPort();
  }

  @Override
  protected InetSocketAddress peerAddress(Connection connection) {
    return new InetSocketAddress(connection.getHost(), connection.getPort());
  }

  @Override
  protected String getInstrumentationName() {
    return "io.opentelemetry.auto.jedis";
  }

  public static final class CommandWithArgs {
    private final ProtocolCommand command;
    private final byte[][] args;

    public CommandWithArgs(ProtocolCommand command, byte[][] args) {
      this.command = command;
      this.args = args;
    }

    private String getStringCommand() {
      if (command instanceof Protocol.Command) {
        return ((Protocol.Command) command).name();
      } else {
        // Protocol.Command is the only implementation in the Jedis lib as of 3.1 but this will save
        // us if that changes
        return new String(command.getRaw(), StandardCharsets.UTF_8);
      }
    }

    private List<String> getStringArgs() {
      return Arrays.stream(args)
          .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
          .collect(Collectors.toList());
    }
  }
}
