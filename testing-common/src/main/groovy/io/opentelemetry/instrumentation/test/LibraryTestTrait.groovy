/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.test

import io.opentelemetry.instrumentation.testing.InstrumentationTestRunner
import io.opentelemetry.instrumentation.testing.LibraryTestRunner

/**
 * A trait which initializes instrumentation library tests, including a test span exporter. All
 * library tests should implement this trait.
 */
trait LibraryTestTrait {

  static final InstrumentationTestRunner instrumentationTestRunner = LibraryTestRunner.instance()

  InstrumentationTestRunner testRunner() {
    instrumentationTestRunner
  }
}
