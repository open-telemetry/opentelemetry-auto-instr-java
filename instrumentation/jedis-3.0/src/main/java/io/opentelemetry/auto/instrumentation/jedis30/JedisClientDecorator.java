package io.opentelemetry.auto.instrumentation.jedis30;

import io.opentelemetry.auto.api.SpanTypes;
import io.opentelemetry.auto.decorator.DatabaseClientDecorator;
import redis.clients.jedis.commands.ProtocolCommand;

public class JedisClientDecorator extends DatabaseClientDecorator<ProtocolCommand> {
  public static final JedisClientDecorator DECORATE = new JedisClientDecorator();

  @Override
  protected String service() {
    return "redis";
  }

  @Override
  protected String getComponentName() {
    return "redis-command";
  }

  @Override
  protected String getSpanType() {
    return SpanTypes.REDIS;
  }

  @Override
  protected String dbType() {
    return "redis";
  }

  @Override
  protected String dbUser(final ProtocolCommand session) {
    return null;
  }

  @Override
  protected String dbInstance(final ProtocolCommand session) {
    return null;
  }
}
