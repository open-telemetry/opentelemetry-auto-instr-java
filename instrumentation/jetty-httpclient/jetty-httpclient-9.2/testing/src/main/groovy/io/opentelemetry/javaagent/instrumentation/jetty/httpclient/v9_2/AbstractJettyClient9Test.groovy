/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jetty.httpclient.v9_2

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import io.opentelemetry.instrumentation.test.base.HttpClientTest
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.ContentResponse
import org.eclipse.jetty.client.api.Request
import org.eclipse.jetty.client.api.Response
import org.eclipse.jetty.client.api.Result
import org.eclipse.jetty.http.HttpMethod
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Shared

import java.util.concurrent.TimeUnit

abstract class AbstractJettyClient9Test extends HttpClientTest<Request> {

  abstract HttpClient createStandardClient()

  abstract HttpClient createHttpsClient(SslContextFactory sslContextFactory)


  @Shared
  def client = createStandardClient()
  @Shared
  def httpsClient = null

  @Rule
  TestName name = new TestName()

  Request jettyRequest = null

  def setupSpec() {

    //Start the main Jetty HttpClient
    client.start()

    SslContextFactory tlsCtx = new SslContextFactory()
//    tlsCtx.setExcludeProtocols("TLSv1.3")
    httpsClient = createHttpsClient(tlsCtx)
    httpsClient.setFollowRedirects(false)
    httpsClient.start()
  }

  @Override
  Request buildRequest(String method, URI uri, Map<String, String> headers) {

    HttpClient theClient = uri.scheme == 'https' ? httpsClient : client

    Request request = theClient.newRequest(uri)

    HttpMethod methodObj = HttpMethod.valueOf(method)
    request.method(methodObj)
    request.timeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)

    jettyRequest = request

    return request
  }

  @Override
  String userAgent() {
    if (name.methodName.startsWith('connection error') && jettyRequest.getAgent() == null) {
      return null
    }
    return "Jetty"
  }

  @Override
  int sendRequest(Request request, String method, URI uri, Map<String, String> headers) {
    headers.each { k, v ->
      request.header(k, v)
    }

    ContentResponse response = request.send()

    return response.status
  }

  private static class JettyClientListener implements Request.FailureListener, Response.FailureListener {

    Throwable failure

    @Override
    void onFailure(Request requestF, Throwable failure) {
      this.failure = failure

    }

    @Override
    void onFailure(Response responseF, Throwable failure) {
      this.failure = failure
    }

  }

  @Override
  void sendRequestWithCallback(Request request, String method, URI uri, Map<String, String> headers, RequestResult requestResult) {

    JettyClientListener jcl = new JettyClientListener()

    request.onRequestFailure(jcl)
    request.onResponseFailure(jcl)
    headers.each { k, v ->
      request.header(k, v)
    }
    Context parentContext = Context.current()
    Scope scope = parentContext.makeCurrent()
//    attachInterceptor(request, parentContext)

    request.send(new Response.CompleteListener() {
      @Override
      void onComplete(Result result) {
        if (jcl.failure != null) {
          requestResult.complete(jcl.failure)
          return
        }

        requestResult.complete(result.response.status)
      }
    })
    scope.close()
  }


  @Override
  boolean testRedirects() {
    false
  }

  @Override
  boolean testCausality() {
    true
  }

  @Override
  Set<AttributeKey<?>> httpAttributes(URI uri) {
    Set<AttributeKey<?>> extra = [
      SemanticAttributes.HTTP_SCHEME,
      SemanticAttributes.HTTP_TARGET,
      SemanticAttributes.HTTP_HOST
    ]
    super.httpAttributes(uri) + extra
  }

}
