/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.annotation.support;

import io.opentelemetry.instrumentation.api.caching.Cache;
import java.lang.reflect.Method;

/** A builder of {@link MethodSpanAttributesExtractor}. */
public final class MethodSpanAttributesExtractorBuilder<REQUEST, RESPONSE> {
  MethodExtractor<REQUEST> methodExtractor;
  MethodArgumentsExtractor<REQUEST> methodArgumentsExtractor;
  Cache<Method, AttributeBindings> cache;
  ParameterAttributeNamesExtractor parameterAttributeNamesExtractor;

  public MethodSpanAttributesExtractorBuilder(MethodExtractor<REQUEST> methodExtractor) {
    this.methodExtractor = methodExtractor;
  }

  /** Sets the {@link Cache} to cache the {@link AttributeBindings} by {@link Method}. */
  public MethodSpanAttributesExtractorBuilder<REQUEST, RESPONSE> setCache(
      Cache<Method, AttributeBindings> cache) {
    this.cache = cache;
    return this;
  }

  /**
   * Sets the {@link ParameterAttributeNamesExtractor} to extract the attribute names from the
   * parameters of the traced method.
   */
  public MethodSpanAttributesExtractorBuilder<REQUEST, RESPONSE>
      setParameterAttributeNamesExtractor(
          ParameterAttributeNamesExtractor parameterAttributeNamesExtractor) {
    this.parameterAttributeNamesExtractor = parameterAttributeNamesExtractor;
    return this;
  }

  /**
   * Returns a new {@link MethodSpanAttributesExtractor} that extracts {@link
   * io.opentelemetry.api.common.Attributes} from the arguments passed to the traced method.
   */
  public MethodSpanAttributesExtractor<REQUEST, RESPONSE> build(
      MethodArgumentsExtractor<REQUEST> methodArgumentsExtractor) {
    this.methodArgumentsExtractor = methodArgumentsExtractor;
    return new MethodSpanAttributesExtractor<>(this);
  }
}
