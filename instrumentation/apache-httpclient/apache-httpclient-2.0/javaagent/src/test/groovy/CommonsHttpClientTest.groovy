/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import io.opentelemetry.instrumentation.test.AgentTestTrait
import io.opentelemetry.instrumentation.test.base.HttpClientTest
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpMethod
import org.apache.commons.httpclient.methods.DeleteMethod
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.HeadMethod
import org.apache.commons.httpclient.methods.OptionsMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.PutMethod
import org.apache.commons.httpclient.methods.TraceMethod
import spock.lang.Shared

class CommonsHttpClientTest extends HttpClientTest<HttpMethod> implements AgentTestTrait {
  @Shared
  HttpClient client = new HttpClient()

  def setupSpec() {
    client.setConnectionTimeout(CONNECT_TIMEOUT_MS)
  }

  @Override
  boolean testCausality() {
    return false
  }

  @Override
  HttpMethod buildRequest(String method, URI uri, Map<String, String> headers) {
    def request
    switch (method) {
      case "GET":
        request = new GetMethod(uri.toString())
        break
      case "PUT":
        request = new PutMethod(uri.toString())
        break
      case "POST":
        request = new PostMethod(uri.toString())
        break
      case "HEAD":
        request = new HeadMethod(uri.toString())
        break
      case "DELETE":
        request = new DeleteMethod(uri.toString())
        break
      case "OPTIONS":
        request = new OptionsMethod(uri.toString())
        break
      case "TRACE":
        request = new TraceMethod(uri.toString())
        break
      default:
        throw new RuntimeException("Unsupported method: " + method)
    }
    headers.each { request.setRequestHeader(it.key, it.value) }
    return request
  }

  @Override
  int sendRequest(HttpMethod request, String method, URI uri, Map<String, String> headers) {
    try {
      client.executeMethod(request)
      def code = request.getStatusCode()
      // apache commons throws an exception if the request is reused without being recycled first
      request.recycle()
      return code
    } finally {
      request.releaseConnection()
    }
  }

  @Override
  boolean testRedirects() {
    // Generates 4 spans
    false
  }

  @Override
  boolean testCallback() {
    false
  }
}
