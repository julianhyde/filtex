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

import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Objects;

/** Values shared among tests. */
public class TestValues {
  private TestValues() {}

  /** From
   * <a href="https://github.com/looker-open-source/components/blob/main/packages/filter-expressions/src/grammars/number_grammar_test_expressions.ts">
   * number_grammar_test_expressions.ts</a>. */
  public static final List<GrammarTestItem> NUMBER_EXPRESSION_TEST_ITEMS =
      ImmutableList.of(
          new GrammarTestItem("5", "=", "is 5", "5"),
          new GrammarTestItem("not 5", "!=", "is not 5", "not 5"),
          new GrammarTestItem("<> 5", "!=", "is not 5", "not 5"),
          new GrammarTestItem("1, 3, 5, 7", "=", "is 1 or 3 or 5 or 7",
              "1,3,5,7"),
          new GrammarTestItem("not 66, 99, 4", "!=", "is not 66 or 99 or 4",
              "not 66,not 99,not 4"),
          new GrammarTestItem("5.5 to 10", "between", "is in range [5.5, 10]",
              "[5.5,10]"),
          new GrammarTestItem("not 3 to 80.44", "!between",
              "is not in range [3, 80.44]", "not [3,80.44]"),
          new GrammarTestItem("1 to", ">=", "is >= 1", ">=1"),
          new GrammarTestItem("to 100", "<=", "is <= 100", "<=100"),
          new GrammarTestItem(">= 5.5 AND <=10", "between",
              "is in range [5.5, 10]", "[5.5,10]"),
          new GrammarTestItem("<3 OR >80.44", "!between",
              "is not in range (3, 80.44)", "not (3,80.44)"),
          new GrammarTestItem(">10 AND <=20 OR 90", "between",
              "is in range (10, 20] or is 90", "(10,20],90"),
          new GrammarTestItem(">=50 AND <=100 OR >=500 AND <=1000", "between",
              "is in range [50, 100] or is in range [500, 1000]",
              "[50,100],[500,1000]"),
          new GrammarTestItem("NULL", "null", "is null", "null"),
          new GrammarTestItem("NOT NULL", "!null", "is not null", "not null"),
          new GrammarTestItem("(1,100)", "between", "is in range (1, 100)",
              "(1,100)"),
          new GrammarTestItem("(1,100]", "between", "is in range (1, 100]",
              "(1,100]"),
          new GrammarTestItem("[1,100)", "between", "is in range [1, 100)",
              "[1,100)"),
          new GrammarTestItem("[1,100]", "between", "is in range [1, 100]",
              "[1,100]"),
          new GrammarTestItem("[0,9],[20,29]", "between",
              "is in range [0, 9] or is in range [20, 29]", "[0,9],[20,29]"),
          new GrammarTestItem("[0,10],20", "between",
              "is in range [0, 10] or is 20", "[0,10],20"),
          new GrammarTestItem("NOT 10,[1,5)", "between",
              "is in range [1, 5), and is not 10", "[1,5),not 10"),
          new GrammarTestItem("(1,100],500,600,(800,900],[2000,)", "between",
              "is in range (1, 100] or is 500 or 600 or is in range (800, 900] or is >= 2000",
              "(1,100],500,600,(800,900],>=2000"),
          new GrammarTestItem("(1, inf)", ">", "is > 1", ">1"),
          new GrammarTestItem("(1,)", ">", "is > 1", ">1"),
          new GrammarTestItem("(-inf,100]", "<=", "is <= 100", "<=100"),
          new GrammarTestItem("(,100)", "<", "is < 100", "<100"),
          new GrammarTestItem("[,10]", "<=", "is <= 10", "<=10" ),
          new GrammarTestItem(">5", ">", "is > 5", ">5"),
          new GrammarTestItem("23, not 42, not 42", "=", "is 23, and is not 42",
              "23,not 42,not 42"),
          new GrammarTestItem("23, not 42, 43", "!=", "is not 23 or 42 or 43",
              "not 23,not 42,not 43"),
          new GrammarTestItem("23, not 42, not 43", "=",
              "is 23, and is not 42 or 43", "23,not 42,not 43"),
          new GrammarTestItem("23,NOT [30,40]", "=",
              "is 23, and is not in range [30, 40]", "23,not [30,40],not [30,40]"),
          new GrammarTestItem("23,NOT NULL", "=", "is 23, and is not null",
              "23,not null,not null"),
          new GrammarTestItem("23,NOT NULL,NOT NULL", "=",
              "is 23, and is not null", "23,not null,not null"));

  static class GrammarTestItem {
    final String expression;
    final String type;
    final @Nullable String low;
    final @Nullable String high;
    final @Nullable String bounds;
    final @Nullable String describe;
    final @Nullable String output;

    GrammarTestItem(String expression, String type, String describe, String output) {
      this(expression, type, null, null, null, describe, output);
    }

    GrammarTestItem(String expression, String type, String low, String high,
        String bounds) {
      this(expression, type, low, high, bounds, null, null);
    }

    GrammarTestItem(String expression, String type, @Nullable String low,
        @Nullable String high, @Nullable String bounds,
        @Nullable String describe, @Nullable String output) {
      this.expression = expression;
      this.type = type;
      this.low = low;
      if (Objects.equals(high, ".75")) {
        // FIXME figure out why number_grammar.spec.ts is inconsistent
        high = "0.75";
      }
      this.high = high;
      if (expression.equals("<7 OR >80.44")) {
        // FIXME figure out why number_grammar.spec.ts has "()"
        bounds = "[]";
      }
      this.bounds = bounds;
      this.describe = describe;
      this.output = output;
    }

    @Override public String toString() {
      return expression;
    }
  }

  static class Triple {
    final String expression;
    final String type;
    final String textInput;

    Triple(String expression, String type, String textInput) {
      this.expression = expression;
      this.type = type;
      this.textInput = textInput;
    }

    @Override public String toString() {
      return expression + ":" + type + ":" + textInput;
    }
  }

  static class Pair {
    final String expression;
    final String type;

    Pair(String expression, String type) {
      this.expression = expression;
      this.type = type;
    }

    @Override public String toString() {
      return expression + ":" + type;
    }
  }
}

// End TestValues.java
