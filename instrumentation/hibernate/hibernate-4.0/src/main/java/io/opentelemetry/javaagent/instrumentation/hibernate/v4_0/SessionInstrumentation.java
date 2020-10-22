/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.hibernate.v4_0;

import static io.opentelemetry.javaagent.instrumentation.hibernate.HibernateDecorator.DECORATE;
import static io.opentelemetry.javaagent.instrumentation.hibernate.SessionMethodUtils.SCOPE_ONLY_METHODS;
import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.hasInterface;
import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.implementsInterface;
import static io.opentelemetry.javaagent.tooling.matcher.NameMatchers.namedOneOf;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.google.auto.service.AutoService;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.instrumentation.api.ContextStore;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.instrumentation.api.SpanWithScope;
import io.opentelemetry.javaagent.instrumentation.hibernate.SessionMethodUtils;
import io.opentelemetry.javaagent.tooling.Instrumenter;
import io.opentelemetry.trace.Span;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SharedSessionContract;
import org.hibernate.Transaction;

@AutoService(Instrumenter.class)
public class SessionInstrumentation extends AbstractHibernateInstrumentation {

  @Override
  public Map<String, String> contextStore() {
    Map<String, String> map = new HashMap<>();
    map.put("org.hibernate.SharedSessionContract", Context.class.getName());
    map.put("org.hibernate.Query", Context.class.getName());
    map.put("org.hibernate.Transaction", Context.class.getName());
    map.put("org.hibernate.Criteria", Context.class.getName());
    return Collections.unmodifiableMap(map);
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named("org.hibernate.SharedSessionContract"));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    Map<ElementMatcher<? super MethodDescription>, String> transformers = new HashMap<>();
    transformers.put(
        isMethod().and(named("close")).and(takesArguments(0)),
        SessionInstrumentation.class.getName() + "$SessionCloseAdvice");

    // Session synchronous methods we want to instrument.
    transformers.put(
        isMethod()
            .and(
                namedOneOf(
                    "save",
                    "replicate",
                    "saveOrUpdate",
                    "update",
                    "merge",
                    "persist",
                    "lock",
                    "refresh",
                    "insert",
                    "delete",
                    // Lazy-load methods.
                    "immediateLoad",
                    "internalLoad")),
        SessionInstrumentation.class.getName() + "$SessionMethodAdvice");
    // Handle the non-generic 'get' separately.
    transformers.put(
        isMethod()
            .and(named("get"))
            .and(returns(named("java.lang.Object")))
            .and(takesArgument(0, named("java.lang.String"))),
        SessionInstrumentation.class.getName() + "$SessionMethodAdvice");

    // These methods return some object that we want to instrument, and so the Advice will pin the
    // current Span to the returned object using a ContextStore.
    transformers.put(
        isMethod()
            .and(namedOneOf("beginTransaction", "getTransaction"))
            .and(returns(named("org.hibernate.Transaction"))),
        SessionInstrumentation.class.getName() + "$GetTransactionAdvice");

    transformers.put(
        isMethod().and(returns(hasInterface(named("org.hibernate.Query")))),
        SessionInstrumentation.class.getName() + "$GetQueryAdvice");

    transformers.put(
        isMethod().and(returns(hasInterface(named("org.hibernate.Criteria")))),
        SessionInstrumentation.class.getName() + "$GetCriteriaAdvice");

    return transformers;
  }

  public static class SessionCloseAdvice extends V4Advice {

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void closeSession(
        @Advice.This SharedSessionContract session, @Advice.Thrown Throwable throwable) {

      ContextStore<SharedSessionContract, Context> contextStore =
          InstrumentationContext.get(SharedSessionContract.class, Context.class);
      Context sessionContext = contextStore.get(session);
      if (sessionContext == null) {
        return;
      }
      Span sessionSpan = Span.fromContext(sessionContext);

      DECORATE.onError(sessionSpan, throwable);
      DECORATE.beforeFinish(sessionSpan);
      sessionSpan.end();
    }
  }

  public static class SessionMethodAdvice extends V4Advice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static SpanWithScope startMethod(
        @Advice.This SharedSessionContract session,
        @Advice.Origin("#m") String name,
        @Advice.Argument(0) Object entity) {

      boolean startSpan = !SCOPE_ONLY_METHODS.contains(name);
      ContextStore<SharedSessionContract, Context> contextStore =
          InstrumentationContext.get(SharedSessionContract.class, Context.class);
      return SessionMethodUtils.startScopeFrom(
          contextStore, session, "Session." + name, entity, startSpan);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void endMethod(
        @Advice.Enter SpanWithScope spanWithScope,
        @Advice.Thrown Throwable throwable,
        @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returned,
        @Advice.Origin("#m") String name) {

      SessionMethodUtils.closeScope(spanWithScope, throwable, "Session." + name, returned);
    }
  }

  public static class GetQueryAdvice extends V4Advice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void getQuery(
        @Advice.This SharedSessionContract session, @Advice.Return Query query) {

      ContextStore<SharedSessionContract, Context> sessionContextStore =
          InstrumentationContext.get(SharedSessionContract.class, Context.class);
      ContextStore<Query, Context> queryContextStore =
          InstrumentationContext.get(Query.class, Context.class);

      SessionMethodUtils.attachSpanFromStore(
          sessionContextStore, session, queryContextStore, query);
    }
  }

  public static class GetTransactionAdvice extends V4Advice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void getTransaction(
        @Advice.This SharedSessionContract session, @Advice.Return Transaction transaction) {

      ContextStore<SharedSessionContract, Context> sessionContextStore =
          InstrumentationContext.get(SharedSessionContract.class, Context.class);
      ContextStore<Transaction, Context> transactionContextStore =
          InstrumentationContext.get(Transaction.class, Context.class);

      SessionMethodUtils.attachSpanFromStore(
          sessionContextStore, session, transactionContextStore, transaction);
    }
  }

  public static class GetCriteriaAdvice extends V4Advice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void getCriteria(
        @Advice.This SharedSessionContract session, @Advice.Return Criteria criteria) {

      ContextStore<SharedSessionContract, Context> sessionContextStore =
          InstrumentationContext.get(SharedSessionContract.class, Context.class);
      ContextStore<Criteria, Context> criteriaContextStore =
          InstrumentationContext.get(Criteria.class, Context.class);

      SessionMethodUtils.attachSpanFromStore(
          sessionContextStore, session, criteriaContextStore, criteria);
    }
  }
}
