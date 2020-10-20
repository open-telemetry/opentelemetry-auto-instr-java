/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.api.db.cassandra;

import io.opentelemetry.javaagent.instrumentation.api.db.sql.normalizer.ParseException;
import io.opentelemetry.javaagent.instrumentation.api.db.sql.normalizer.SqlNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CassandraQueryNormalizer {
  private static final Logger log = LoggerFactory.getLogger(CassandraQueryNormalizer.class);

  public static String normalize(String query) {
    try {
      return SqlNormalizer.normalize(query);
    } catch (ParseException e) {
      log.debug("Could not normalize Cassandra query", e);
      return null;
    }
  }

  private CassandraQueryNormalizer() {}
}
