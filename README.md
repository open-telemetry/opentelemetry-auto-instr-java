---

<p align="center">
  <strong>
    <a href="https://github.com/open-telemetry/opentelemetry-java-instrumentation#getting-started">Getting Started</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="https://github.com/open-telemetry/community#special-interest-groups">Getting Involved</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/discussions">Getting In Touch</a>
  </strong>
</p>

<p align="center">
  <a href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/actions?query=workflow%3A%22Nightly+build%22">
    <img alt="Build Status" src="https://img.shields.io/github/workflow/status/open-telemetry/opentelemetry-java-instrumentation/Nightly%20build?style=for-the-badge">
  </a>
  <a href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases">
    <img alt="GitHub release (latest by date including pre-releases)" src="https://img.shields.io/github/v/release/open-telemetry/opentelemetry-java-instrumentation?include_prereleases&style=for-the-badge">
  </a>
  <a href="https://bintray.com/open-telemetry/maven/opentelemetry-java-instrumentation">
    <img alt="Bintray" src="https://img.shields.io/bintray/v/open-telemetry/maven/opentelemetry-java-instrumentation?style=for-the-badge">
  </a>
  <img alt="Beta" src="https://img.shields.io/badge/status-beta-informational?style=for-the-badge&logo=data:image/png;base64,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">
</p>

<p align="center">
  <strong>
    <a href="CONTRIBUTING.md">Contributing<a/>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="docs/scope.md">Scope<a/>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="docs/ga-requirements.md">Roadmap<a/>
  </strong>
</p>

---

# <img src="https://opentelemetry.io/img/logos/opentelemetry-logo-nav.png" alt="OpenTelemetry Icon" width="45" height=""> OpenTelemetry Instrumentation for Java

This project provides a Java agent JAR that can be attached to any Java 8+
application and dynamically injects bytecode to capture telemetry from a
number of popular libraries and frameworks.
You can export the telemetry data in a variety of formats.
You can also configure the agent and exporter via command line arguments
or environment variables. The net result is the ability to gather telemetry
data from a Java application without code changes.

## Getting Started

Download the [latest version](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent-all.jar).

This package includes the instrumentation agent as well as
instrumentations for all supported libraries and all available data exporters.
The package provides a completely automatic, out-of-the-box experience.

Enable the instrumentation agent using the `-javaagent` flag to the JVM.
```
java -javaagent:path/to/opentelemetry-javaagent-all.jar \
     -jar myapp.jar
```
By default, the OpenTelemetry Java agent uses
[OTLP exporter](https://github.com/open-telemetry/opentelemetry-java/tree/master/exporters/otlp)
configured to send data to
[OpenTelemetry collector](https://github.com/open-telemetry/opentelemetry-collector/blob/master/receiver/otlpreceiver/README.md)
at `http://localhost:4317`.

Configuration parameters are passed as Java system properties (`-D` flags) or
as environment variables. See below for a full list of environment variables. For example:
```
java -javaagent:path/to/opentelemetry-javaagent-all.jar \
     -Dotel.trace.exporter=zipkin \
     -jar myapp.jar
```

Specify the external exporter JAR file using the `otel.exporter.jar` system property:
```
java -javaagent:path/to/opentelemetry-javaagent-all.jar \
     -Dotel.exporter.jar=path/to/external-exporter.jar \
     -jar myapp.jar
```

Learn how to add custom instrumentation in the [Manually Instrumenting](#manually-instrumenting)
section.

## Configuring the Agent

The agent is [highly configurable](docs/agent-config.md)!  Many aspects of the agent's behavior can be
configured for your needs, such as exporter choice, exporter config (like where
data is sent), trace context propagation headers, and much more.

[Click here to see the detailed list of configuration environment variables and system properties](docs/agent-config.md).

*Note: Config parameter names are very likely to change over time, so please check
back here when trying out a new version! Please [report any bugs](https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues) or unexpected
behavior you find.*

## Manually instrumenting

> :warning: starting with 0.6.0, and prior to version 1.0.0, `opentelemetry-javaagent-all.jar`
only supports manual instrumentation using the `opentelemetry-api` version with the same version
number as the Java agent you are using. Starting with 1.0.0, the Java agent will start supporting
multiple (1.0.0+) versions of `opentelemetry-api`.

You'll need to add a dependency on the `opentelemetry-api` library to get started; if you intend to
use the `@WithSpan` annotation, also include the `opentelemetry-extension-annotations` dependency.

### Maven

```xml
  <dependencies>
    <dependency>
      <groupId>io.opentelemetry</groupId>
      <artifactId>opentelemetry-api</artifactId>
      <version>0.15.0</version>
    </dependency>
    <dependency>
      <groupId>io.opentelemetry</groupId>
      <artifactId>opentelemetry-extension-annotations</artifactId>
      <version>0.15.0</version>
    </dependency>
  </dependencies>
```

### Gradle

```groovy
dependencies {
    implementation('io.opentelemetry:opentelemetry-api:0.15.0')
    implementation('io.opentelemetry:opentelemetry-extension-annotations:0.15.0')
}
```

### Adding attributes to the current span

A common need when instrumenting an application is to capture additional application-specific or
business-specific information as additional attributes to an existing span from the automatic
instrumentation. Grab the current span with `Span.current()` and use the `setAttribute()`
methods:

```java
import io.opentelemetry.api.trace.Span;

// ...

Span span = Span.current();
span.setAttribute(..., ...);
```

### Creating spans around methods with `@WithSpan`

Another common situation is to capture a span around an existing first-party code method. The
`@WithSpan` annotation makes this straightforward:

```java
import io.opentelemetry.extension.annotations.WithSpan;

public class MyClass {
  @WithSpan
  public void MyLogic() {
      <...>
  }
}
```

Each time the application invokes the annotated method, it creates a span that denote its duration
and provides any thrown exceptions. Unless specified as an argument to the annotation, the span name
will be `<className>.<methodName>`.

#### Suppressing `@WithSpan` instrumentation

Suppressing `@WithSpan` is useful if you have code that is over-instrumented using `@WithSpan`
and you want to suppress some of them without modifying the code.

| System property                 | Environment variable            | Purpose                                                                                                                                  |
|---------------------------------|---------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| trace.annotated.methods.exclude | TRACE_ANNOTATED_METHODS_EXCLUDE | Suppress `@WithSpan` instrumentation for specific methods.
Format is "my.package.MyClass1[method1,method2];my.package.MyClass2[method3]" |

#### Creating spans around methods with otel.instrumentation.methods.include
This is a way to to create a span around a first-party code method without using `@WithSpan`.

| System property                 | Environment variable            | Purpose                                                                                                                                  |
|---------------------------------|---------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| otel.instrumentation.methods.include |                                 | Add instrumentation for specific methods in lieu of `@WithSpan`.
Format is "my.package.MyClass1[method1,method2];my.package.MyClass2[method3]" |

### Creating spans manually with a Tracer

OpenTelemetry offers a tracer to easily enable custom instrumentation throughout your application.
See the [OpenTelemetry Java
QuickStart](https://github.com/open-telemetry/opentelemetry-java/blob/master/QUICKSTART.md#tracing)
for an example of how to configure the tracer and use the Tracer, Scope and Span interfaces to
instrument your application.

## Supported libraries, frameworks, and application servers

We support an impressively huge number of [libraries and frameworks](docs/supported-libraries.md#libraries---frameworks) and
a majority of the most popular [application servers](docs/supported-libraries.md#application-servers)...right out of the box!
[Click here to see the full list](docs/supported-libraries.md) and to learn more about
[disabled instrumentation](docs/supported-libraries.md#disabled-instrumentations)
and how to [suppress unwanted instrumentation](docs/suppressing-instrumentation.md).


## Troubleshooting

To turn on the agent's internal debug logging:

`-Dotel.javaagent.debug=true`

**Note**: These logs are extremely verbose. Enable debug logging only when needed.
Debug logging negatively impacts the performance of your application.

## Roadmap to 1.0 (GA)

See [GA Requirements](docs/ga-requirements.md)

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

Approvers ([@open-telemetry/java-instrumentation-approvers](https://github.com/orgs/open-telemetry/teams/java-instrumentation-approvers)):

- [John Watson](https://github.com/jkwatson), Splunk
- [Mateusz Rzeszutek](https://github.com/mateuszrzeszutek), Splunk
- [Pavol Loffay](https://github.com/pavolloffay), Traceable.ai

Maintainers ([@open-telemetry/java-instrumentation-maintainers](https://github.com/orgs/open-telemetry/teams/java-instrumentation-maintainers)):

- [Anuraag Agrawal](https://github.com/anuraaga), AWS
- [Nikita Salnikov-Tarnovski](https://github.com/iNikem), Splunk
- [Trask Stalnaker](https://github.com/trask), Microsoft
- [Tyler Benson](https://github.com/tylerbenson), DataDog

Learn more about roles in the [community repository](https://github.com/open-telemetry/community/blob/master/community-membership.md).

Thanks to all the people who already contributed!

<a href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/graphs/contributors">
  <img src="https://contributors-img.web.app/image?repo=open-telemetry/opentelemetry-java-instrumentation" />
</a>
