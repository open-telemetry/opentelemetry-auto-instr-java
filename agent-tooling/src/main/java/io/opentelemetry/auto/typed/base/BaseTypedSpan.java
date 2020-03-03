package io.opentelemetry.auto.typed.base;

import io.opentelemetry.trace.EndSpanOptions;
import io.opentelemetry.trace.Span;

public abstract class BaseTypedSpan<T extends BaseTypedSpan> extends DelegatingSpan {

  public BaseTypedSpan(Span delegate) {
    super(delegate);
  }

  public void end(Throwable throwable) {
    // add error details to the span.
    super.end();
  }

  /**
   * The end(Throwable), or end(RESPONSE) methods should be used instead.
   */
  @Deprecated
  @Override
  public void end() {
    super.end();
  }

  /**
   * The end(Throwable), or end(RESPONSE) methods should be used instead.
   */
  @Deprecated
  @Override
  public void end(EndSpanOptions endOptions) {
    super.end(endOptions);
  }

  protected abstract T self();
}
