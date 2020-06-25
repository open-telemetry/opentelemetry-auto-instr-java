/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.auto.instrumentation.play.v2_3;

import io.opentelemetry.context.propagation.HttpTextFormat;
import play.api.mvc.Headers;
import scala.Option;

public class PlayHeaders implements HttpTextFormat.Getter<Headers> {

  public static final PlayHeaders GETTER = new PlayHeaders();

  @Override
  public String get(final Headers headers, final String key) {
    final Option<String> option = headers.get(key);
    if (option.isDefined()) {
      return option.get();
    } else {
      return null;
    }
  }
}
