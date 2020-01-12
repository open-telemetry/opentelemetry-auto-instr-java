package datadog.trace.agent.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.collect.Sets;
import datadog.trace.agent.test.asserts.ListWriterAssert;
import datadog.trace.agent.tooling.AgentInstaller;
import datadog.trace.agent.tooling.AgentTracerImpl;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.instrumentation.api.AgentTracer;
import datadog.trace.util.test.DDSpecification;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.SimpleType;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.TraceId;
import io.opentelemetry.trace.Tracer;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import org.spockframework.runtime.model.SpecMetadata;

/**
 * A spock test runner which automatically applies instrumentation and exposes a global trace
 * writer.
 *
 * <p>To use, write a regular spock test, but extend this class instead of {@link
 * spock.lang.Specification}. <br>
 * This will cause the following to occur before test startup:
 *
 * <ul>
 *   <li>All {@link Instrumenter}s on the test classpath will be applied. Matching preloaded classes
 *       will be retransformed.
 *   <li>{@link AgentTestRunner#TEST_WRITER} will be registerd with the global tracer and available
 *       in an initialized state.
 * </ul>
 */
@RunWith(SpockRunner.class)
@SpecMetadata(filename = "AgentTestRunner.java", line = 0)
@Slf4j
public abstract class AgentTestRunner extends DDSpecification {
  private static final long TIMEOUT_MILLIS = 10 * 1000;
  /**
   * For test runs, agent's global tracer will report to this list writer.
   *
   * <p>Before the start of each test the reported traces will be reset.
   */
  public static final ListWriter TEST_WRITER;

  protected static final Tracer TEST_TRACER;

  protected static final Set<String> TRANSFORMED_CLASSES = Sets.newConcurrentHashSet();
  private static final AtomicInteger INSTRUMENTATION_ERROR_COUNT = new AtomicInteger();
  private static final TestRunnerListener TEST_LISTENER = new TestRunnerListener();

  private static final Instrumentation INSTRUMENTATION;
  private static volatile ClassFileTransformer activeTransformer = null;

  static {
    INSTRUMENTATION = ByteBuddyAgent.getInstrumentation();

    ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
    ((Logger) LoggerFactory.getLogger("datadog")).setLevel(Level.DEBUG);

    TEST_WRITER = new ListWriter();
    OpenTelemetrySdk.getTracerFactory()
        .addSpanProcessor(SimpleSpansProcessor.newBuilder(TEST_WRITER).build());
    TEST_TRACER = OpenTelemetry.getTracerFactory().get("io.opentelemetry.auto");

    AgentTracer.registerIfAbsent(new AgentTracerImpl(TEST_TRACER));
  }

  protected static Tracer getTestTracer() {
    return TEST_TRACER;
  }

  protected static ListWriter getTestWriter() {
    return TEST_WRITER;
  }

  /**
   * Invoked when Bytebuddy encounters an instrumentation error. Fails the test by default.
   *
   * <p>Override to skip specific expected errors.
   *
   * @return true if the test should fail because of this error.
   */
  protected boolean onInstrumentationError(
      final String typeName,
      final ClassLoader classLoader,
      final JavaModule module,
      final boolean loaded,
      final Throwable throwable) {
    log.error(
        "Unexpected instrumentation error when instrumenting {} on {}",
        typeName,
        classLoader,
        throwable);
    return true;
  }

  /**
   * @param className name of the class being loaded
   * @param classLoader classloader class is being defined on
   * @return true if the class under load should be transformed for this test.
   */
  protected boolean shouldTransformClass(final String className, final ClassLoader classLoader) {
    return true;
  }

  @BeforeClass
  public static synchronized void agentSetup() throws Exception {
    if (null != activeTransformer) {
      throw new IllegalStateException("transformer already in place: " + activeTransformer);
    }

    final ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(AgentTestRunner.class.getClassLoader());
      assert ServiceLoader.load(Instrumenter.class).iterator().hasNext()
          : "No instrumentation found";
      activeTransformer = AgentInstaller.installBytebuddyAgent(INSTRUMENTATION, TEST_LISTENER);
    } finally {
      Thread.currentThread().setContextClassLoader(contextLoader);
    }

    INSTRUMENTATION_ERROR_COUNT.set(0);
  }

  /**
   * Normally {@code @BeforeClass} is run only on static methods, but spock allows us to run it on
   * instance methods. Note: this means there is a 'special' instance of test class that is not used
   * to run any tests, but instead is just used to run this method once.
   */
  @BeforeClass
  public void setupBeforeTests() {
    TEST_LISTENER.activateTest(this);
  }

  @Before
  public void beforeTest() {
    assert !getTestTracer().getCurrentSpan().getContext().isValid()
        : "Span is active before test has started: " + getTestTracer().getCurrentSpan();
    log.debug("Starting test: '{}'", getSpecificationContext().getCurrentIteration().getName());
    TEST_WRITER.clear();
  }

  /** See comment for {@code #setupBeforeTests} above. */
  @AfterClass
  public void cleanUpAfterTests() {
    TEST_LISTENER.deactivateTest(this);
  }

  @AfterClass
  public static synchronized void agentCleanup() {
    if (null != activeTransformer) {
      INSTRUMENTATION.removeTransformer(activeTransformer);
      activeTransformer = null;
    }
    // Cleanup before assertion.
    assert INSTRUMENTATION_ERROR_COUNT.get() == 0
        : INSTRUMENTATION_ERROR_COUNT.get() + " Instrumentation errors during test";
  }

  public static void assertTraces(
      final int size,
      @ClosureParams(
              value = SimpleType.class,
              options = "datadog.trace.agent.test.asserts.ListWriterAssert")
          @DelegatesTo(value = ListWriterAssert.class, strategy = Closure.DELEGATE_FIRST)
          final Closure spec) {
    ListWriterAssert.assertTraces(TEST_WRITER, size, spec);
  }

  @SneakyThrows
  public static void blockUntilChildSpansFinished(final int numberOfSpans) {
    final Span span = getTestTracer().getCurrentSpan();
    final long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
    if (span.getContext().isValid()) {
      final TraceId traceId = span.getContext().getTraceId();
      int foundSpans = 0;
      while (System.currentTimeMillis() < deadline) {
        for (final List<SpanData> trace : TEST_WRITER) {
          if (trace.get(0).getTraceId().equals(traceId)) {
            foundSpans = trace.size();
            if (foundSpans >= numberOfSpans) {
              return;
            } else {
              break; // breaks inner for loop
            }
          }
        }
        Thread.sleep(10);
      }
      throw new TimeoutException("Timed out waiting for child spans.  Received: " + foundSpans);
    }
  }

  public static class TestRunnerListener implements AgentBuilder.Listener {
    private static final List<AgentTestRunner> activeTests = new CopyOnWriteArrayList<>();

    public void activateTest(final AgentTestRunner testRunner) {
      activeTests.add(testRunner);
    }

    public void deactivateTest(final AgentTestRunner testRunner) {
      activeTests.remove(testRunner);
    }

    @Override
    public void onDiscovery(
        final String typeName,
        final ClassLoader classLoader,
        final JavaModule module,
        final boolean loaded) {
      for (final AgentTestRunner testRunner : activeTests) {
        if (!testRunner.shouldTransformClass(typeName, classLoader)) {
          throw new AbortTransformationException(
              "Aborting transform for class name = " + typeName + ", loader = " + classLoader);
        }
      }
    }

    @Override
    public void onTransformation(
        final TypeDescription typeDescription,
        final ClassLoader classLoader,
        final JavaModule module,
        final boolean loaded,
        final DynamicType dynamicType) {
      TRANSFORMED_CLASSES.add(typeDescription.getActualName());
    }

    @Override
    public void onIgnored(
        final TypeDescription typeDescription,
        final ClassLoader classLoader,
        final JavaModule module,
        final boolean loaded) {}

    @Override
    public void onError(
        final String typeName,
        final ClassLoader classLoader,
        final JavaModule module,
        final boolean loaded,
        final Throwable throwable) {
      if (!(throwable instanceof AbortTransformationException)) {
        for (final AgentTestRunner testRunner : activeTests) {
          if (testRunner.onInstrumentationError(typeName, classLoader, module, loaded, throwable)) {
            INSTRUMENTATION_ERROR_COUNT.incrementAndGet();
            break;
          }
        }
      }
    }

    @Override
    public void onComplete(
        final String typeName,
        final ClassLoader classLoader,
        final JavaModule module,
        final boolean loaded) {}

    /** Used to signal that a transformation was intentionally aborted and is not an error. */
    public static class AbortTransformationException extends RuntimeException {
      public AbortTransformationException() {
        super();
      }

      public AbortTransformationException(final String message) {
        super(message);
      }
    }
  }
}
