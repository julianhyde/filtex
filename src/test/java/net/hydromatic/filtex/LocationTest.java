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
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

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
    final SortedMap<String, Object> map = new TreeMap<>();

    Digester put(String key, Object value) {
      map.put(key, value);
      return this;
    }

    @Override public String toString() {
      return map.toString();
    }

    <T> Digester putIfInstance(String key, Class<T> clazz, Object o,
        Function<T, Object> f) {
      if (clazz.isInstance(o)) {
        map.put(key, f.apply(clazz.cast(o)));
      }
      return this;
    }
  }

}

// End LocationTest.java
