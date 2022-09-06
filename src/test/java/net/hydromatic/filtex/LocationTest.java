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
package net.hydromatic.filtex;

import net.hydromatic.filtex.ast.Ast;
import net.hydromatic.filtex.ast.AstNode;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static net.hydromatic.filtex.Filtex.parseFilterExpression;
import static net.hydromatic.filtex.TestValues.forEach;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/** Tests location expressions. */
public class LocationTest {

  void checkLocationItem(String expression, String expectedSummary,
      @Nullable String expectedDigest) {
    final AstNode ast =
        parseFilterExpression(TypeFamily.LOCATION, expression);
    String summary =
        Filtex.summary(TypeFamily.LOCATION, expression, Locale.ENGLISH);
    assertThat(summary, is(expectedSummary));
    if (expectedDigest != null) {
      final String digest = digest(ast);
      assertThat(digest, is(expectedDigest));
    }
  }

  private String digest(AstNode node) {
    return new Digester()
        .put("type", node.type())
        .putIfInstance("lat", Ast.Point.class, node, p ->
            p.location.latitude)
        .putIfInstance("long", Ast.Point.class, node, p ->
            p.location.longitude)
        .putIfInstance("distance", Ast.Circle.class, node, c ->
            c.distance)
        .putIfInstance("unit", Ast.Circle.class, node, c ->
            c.unit.plural)
        .putIfInstance("lat", Ast.Circle.class, node, c ->
            c.location.latitude)
        .putIfInstance("lon", Ast.Circle.class, node, c ->
            c.location.longitude)
        .putIfInstance("lat", Ast.Box.class, node, box ->
            box.from.latitude)
        .putIfInstance("lon", Ast.Box.class, node, box ->
            box.from.longitude)
        .putIfInstance("lat1", Ast.Box.class, node, c ->
            c.to.latitude)
        .putIfInstance("lon1", Ast.Box.class, node, c ->
            c.to.longitude)
        .toString();
  }

  @Test void testLocationGrammarCanParse() {
    forEach(TestValues.LOCATION_EXPRESSION_TEST_ITEMS, item ->
        checkLocationItem(item.expression, item.type, item.textInput));
  }

  /** Helps generate a digest of the properties in an AstNode. */
  static class Digester {
    static final Pattern ALPHANUMERIC = Pattern.compile("[a-zA-Z0-9_]*");

    final SortedMap<String, Object> map = new TreeMap<>();

    Digester put(String key, Object value) {
      map.put(key, value);
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

    <T> Digester putIfInstance(String key, Class<T> clazz, Object o,
        Function<T, @Nullable Object> f) {
      return putIfInstanceIf(key, clazz, o, t -> true, f);
    }

    <T> Digester putIfInstanceIf(String key, Class<T> clazz, Object o,
        Predicate<T> predicate,
        Function<T, @Nullable Object> f) {
      if (clazz.isInstance(o)) {
        T t = clazz.cast(o);
        if (predicate.test(t)) {
          @Nullable Object value = f.apply(t);
          if (value != null) {
            map.put(key, value);
          }
        }
      }
      return this;
    }
  }

}

// End LocationTest.java
