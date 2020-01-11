package datadog.trace.instrumentation.hystrix;

import com.netflix.hystrix.HystrixInvokableInfo;
import datadog.trace.agent.decorator.BaseDecorator;
import datadog.trace.api.MoreTags;
import datadog.trace.instrumentation.api.AgentSpan;

public class HystrixDecorator extends BaseDecorator {
  public static HystrixDecorator DECORATE = new HystrixDecorator();

  @Override
  protected String[] instrumentationNames() {
    return new String[0];
  }

  @Override
  protected String spanType() {
    return null;
  }

  @Override
  protected String component() {
    return "hystrix";
  }

  public void onCommand(
      final AgentSpan span, final HystrixInvokableInfo<?> command, final String methodName) {
    if (command != null) {
      final String commandName = command.getCommandKey().name();
      final String groupName = command.getCommandGroup().name();
      final boolean circuitOpen = command.isCircuitBreakerOpen();

      final String resourceName = groupName + "." + commandName + "." + methodName;

      span.setTag(MoreTags.RESOURCE_NAME, resourceName);
      span.setTag("hystrix.command", commandName);
      span.setTag("hystrix.group", groupName);
      span.setTag("hystrix.circuit-open", circuitOpen);
    }
  }
}
