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

import net.hydromatic.filtex.ast.AstNode;
import net.hydromatic.filtex.ast.Digester;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Locale;

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
      final String digest = ast.digest(new Digester()).toString();
      assertThat(digest, is(expectedDigest));
    }
  }

  @Test void testLocationGrammarCanParse() {
    forEach(TestValues.LOCATION_EXPRESSION_TEST_ITEMS, item ->
        checkLocationItem(item.expression, item.type, item.textInput));
  }
}

// End LocationTest.java
