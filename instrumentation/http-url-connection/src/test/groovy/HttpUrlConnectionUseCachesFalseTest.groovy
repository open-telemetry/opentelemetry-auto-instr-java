/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import io.opentelemetry.instrumentation.test.base.HttpClientTest
import io.opentelemetry.trace.Span
import spock.lang.Timeout

@Timeout(5)
class HttpUrlConnectionUseCachesFalseTest extends HttpClientTest {

  @Override
  int doRequest(String method, URI uri, Map<String, String> headers, Closure callback) {
    HttpURLConnection connection = uri.toURL().openConnection()
    try {
      connection.setRequestMethod(method)
      headers.each { connection.setRequestProperty(it.key, it.value) }
      connection.setRequestProperty("Connection", "close")
      connection.useCaches = false
      connection.connectTimeout = CONNECT_TIMEOUT_MS
      def parentSpan = Span.current()
      def stream = connection.inputStream
      assert Span.current() == parentSpan
      stream.readLines()
      stream.close()
      callback?.call()
      return connection.getResponseCode()
    } finally {
      connection.disconnect()
    }
  }

  @Override
  boolean testCircularRedirects() {
    false
  }
}
