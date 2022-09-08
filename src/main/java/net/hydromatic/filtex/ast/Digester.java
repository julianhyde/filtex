/*
 * Licensed to Julian Hyde under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hydromatic.filtex.ast;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** Helps generate a digest of the properties in an AstNode. */
public class Digester {
  static final Pattern ALPHANUMERIC = Pattern.compile("[a-zA-Z0-9_]*");

  final SortedMap<String, Object>  map;
  final String prefix;

  /** Creates an empty Digester. */
  public Digester() {
    this(new TreeMap<>(), "");
  }

  private Digester(SortedMap<String, Object> map, String prefix) {
    this.map = map;
    this.prefix = prefix;
  }

  /** Creates a digester with the same map, additional prefix. */
  private Digester plus(String prefix) {
    return new Digester(map, this.prefix + prefix + ".");
  }

  /** Adds a value. */
  public Digester put(String key, Object value) {
    map.put(prefix + key, value);
    return this;
  }

  /** Adds a value if it is not null. */
  public Digester putIf(String key, @Nullable Object value) {
    if (value != null) {
      put(key, value);
    }
    return this;
  }

  /** Adds an AST sub-object. */
  public Digester sub(String key, AstNode node) {
    node.digest(plus(key));
    return this;
  }

  /** Adds a Date sub-object. */
  public Digester sub(String key, Date date) {
    date.digest(plus(key));
    return this;
  }

  @Override public String toString() {
    // Convert map "{a=p,q, b.x=2, b.y=3}" to map2 "{a='p,q', b={x=2, y=3}}"
    final SortedMap<String, Object> map2 = new TreeMap<>();
    map.forEach((key, value) -> {
      if (value instanceof String
          && !ALPHANUMERIC.matcher((String) value).matches()) {
        value = "'" + value + "'";
      }
      if (key.contains(".")) {
        final int i = key.indexOf('.');
        @SuppressWarnings("unchecked") final Map<String, Object> subMap =
            (Map<String, Object>)
                map2.computeIfAbsent(key.substring(0, i),
                    k -> new TreeMap<>());
        subMap.put(key.substring(i + 1), value);
      } else {
        map2.put(key, value);
      }
    });
    return map2.toString();
  }

}
