package io.opentelemetry.auto.instrumentation.lettuce.rx;

import static io.opentelemetry.auto.instrumentation.lettuce.LettuceClientDecorator.DECORATE;
import static io.opentelemetry.auto.instrumentation.lettuce.LettuceClientDecorator.TRACER;
import static io.opentelemetry.trace.Span.Kind.CLIENT;

import io.lettuce.core.protocol.RedisCommand;
import io.opentelemetry.trace.Span;
import java.util.function.Consumer;
import org.reactivestreams.Subscription;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Signal;
import reactor.core.publisher.SignalType;

public class LettuceFluxTerminationRunnable implements Consumer<Signal>, Runnable {

  private Span span = null;
  private int numResults = 0;
  private FluxOnSubscribeConsumer onSubscribeConsumer = null;

  public LettuceFluxTerminationRunnable(
      final RedisCommand command, final boolean finishSpanOnClose) {
    onSubscribeConsumer = new FluxOnSubscribeConsumer(this, command, finishSpanOnClose);
  }

  public FluxOnSubscribeConsumer getOnSubscribeConsumer() {
    return onSubscribeConsumer;
  }

  private void finishSpan(final boolean isCommandCancelled, final Throwable throwable) {
    if (span != null) {
      span.setAttribute("db.command.results.count", numResults);
      if (isCommandCancelled) {
        span.setAttribute("db.command.cancelled", true);
      }
      DECORATE.onError(span, throwable);
      DECORATE.beforeFinish(span);
      span.end();
    } else {
      LoggerFactory.getLogger(Flux.class)
          .error(
              "Failed to finish this.span, LettuceFluxTerminationRunnable cannot find this.span because "
                  + "it probably wasn't started.");
    }
  }

  @Override
  public void accept(final Signal signal) {
    if (SignalType.ON_COMPLETE.equals(signal.getType())
        || SignalType.ON_ERROR.equals(signal.getType())) {
      finishSpan(false, signal.getThrowable());
    } else if (SignalType.ON_NEXT.equals(signal.getType())) {
      ++numResults;
    }
  }

  @Override
  public void run() {
    if (span != null) {
      finishSpan(true, null);
    } else {
      LoggerFactory.getLogger(Flux.class)
          .error(
              "Failed to finish this.span to indicate cancellation, LettuceFluxTerminationRunnable cannot find this.span because "
                  + "it probably wasn't started.");
    }
  }

  public static class FluxOnSubscribeConsumer implements Consumer<Subscription> {

    private final LettuceFluxTerminationRunnable owner;
    private final RedisCommand command;
    private final boolean finishSpanOnClose;

    public FluxOnSubscribeConsumer(
        final LettuceFluxTerminationRunnable owner,
        final RedisCommand command,
        final boolean finishSpanOnClose) {
      this.owner = owner;
      this.command = command;
      this.finishSpanOnClose = finishSpanOnClose;
    }

    @Override
    public void accept(final Subscription subscription) {
      final Span span = TRACER.spanBuilder("redis.query").setSpanKind(CLIENT).startSpan();
      owner.span = span;
      DECORATE.afterStart(span);
      DECORATE.onCommand(span, command);
      if (finishSpanOnClose) {
        DECORATE.beforeFinish(span);
        span.end();
      }
    }
  }
}
