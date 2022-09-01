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
import java.util.function.Consumer;

/** Values shared among tests. */
public class TestValues {
  private TestValues() {}

  /** From
   * <a href="https://github.com/looker-open-source/components/blob/main/packages/filter-expressions/src/grammars/location_grammar.spec.ts">
   * location_grammar.spec.ts</a>. */
  public static final List<Triple> LOCATION_EXPRESSION_TEST_ITEMS =
      Triple.builder()
          .add("36.97, -122.03", "36.97, -122.03",
              "{lat=36.97, long=-122.03, type=location}")
          .add("-36.97, 122.03", "-36.97, 122.03",
              "{lat=-36.97, long=122.03, type=location}")
          .add("-36.97, -122.03", "-36.97, -122.03",
              "{lat=-36.97, long=-122.03, type=location}")
          .add("40 miles from -36.97, -122.03", "40 miles from -36.97, -122.03",
              "{distance=40, lat=-36.97, lon=-122.03, type=circle, unit=miles}")
          .add("40 miles from 36.97, -122.03", "40 miles from 36.97, -122.03",
              "{distance=40, lat=36.97, lon=-122.03, type=circle, unit=miles}")
          .add("100 miles from 36.97, -122.03", "100 miles from 36.97, -122.03",
              "{distance=100, lat=36.97, lon=-122.03, type=circle, unit=miles}")
          .add("inside box from 72.33, -173.14 to 14.39, -61.70",
              "72.3°N, 173.1°W to 14.4°N, 61.7°W",
              "{lat=72.33, lat1=14.39, lon=-173.14, lon1=-61.70, type=box}")
          .add("", "is anywhere", null)
          .add("NOT NULL", "is not null", "{type=notnull}")
          .add("-NULL", "is not null", "{type=notnull}")
          .add("NULL", "is null", "{type=null}")
          .build();

  /** From
   * <a href="https://github.com/looker-open-source/components/blob/main/packages/filter-expressions/src/grammars/number_grammar_test_expressions.ts">
   * number_grammar_test_expressions.ts</a>. */
  public static final List<GrammarTestItem> NUMBER_EXPRESSION_TEST_ITEMS =
      GrammarTestItem.builder()
          .add("5", "=", "is 5", "5")
          .add("not 5", "!=", "is not 5", "not 5")
          .add("<> 5", "!=", "is not 5", "not 5")
          .add("1, 3, 5, 7", "=", "is 1 or 3 or 5 or 7", "1,3,5,7")
          .add("not 66, 99, 4", "!=", "is not 66 or 99 or 4",
              "not 66,not 99,not 4")
          .add("5.5 to 10", "between", "is in range [5.5, 10]", "[5.5,10]")
          .add("not 3 to 80.44", "!between",
              "is not in range [3, 80.44]", "not [3,80.44]")
          .add("1 to", ">=", "is >= 1", ">=1")
          .add("to 100", "<=", "is <= 100", "<=100")
          .add(">= 5.5 AND <=10", "between",
              "is in range [5.5, 10]", "[5.5,10]")
          .add("<3 OR >80.44", "!between",
              "is not in range (3, 80.44)", "not (3,80.44)")
          .add(">10 AND <=20 OR 90", "between",
              "is in range (10, 20] or is 90", "(10,20],90")
          .add(">=50 AND <=100 OR >=500 AND <=1000", "between",
              "is in range [50, 100] or is in range [500, 1000]",
              "[50,100],[500,1000]")
          .add("NULL", "null", "is null", "null")
          .add("NOT NULL", "!null", "is not null", "not null")
          .add("(1,100)", "between", "is in range (1, 100)", "(1,100)")
          .add("(1,100]", "between", "is in range (1, 100]", "(1,100]")
          .add("[1,100)", "between", "is in range [1, 100)", "[1,100)")
          .add("[1,100]", "between", "is in range [1, 100]", "[1,100]")
          .add("[0,9],[20,29]", "between",
              "is in range [0, 9] or is in range [20, 29]", "[0,9],[20,29]")
          .add("[0,10],20", "between", "is in range [0, 10] or is 20",
              "[0,10],20")
          .add("NOT 10,[1,5)", "between", "is in range [1, 5), and is not 10",
              "[1,5),not 10")
          .add("(1,100],500,600,(800,900],[2000,)", "between",
              "is in range (1, 100] or is 500 or 600 or is in range (800, 900] or is >= 2000",
              "(1,100],500,600,(800,900],>=2000")
          .add("(1, inf)", ">", "is > 1", ">1")
          .add("(1,)", ">", "is > 1", ">1")
          .add("(-inf,100]", "<=", "is <= 100", "<=100")
          .add("(,100)", "<", "is < 100", "<100")
          .add("[,10]", "<=", "is <= 10", "<=10")
          .add(">5", ">", "is > 5", ">5")
          .add("23, not 42, not 42", "=", "is 23, and is not 42",
              "23,not 42,not 42")
          .add("23, not 42, 43", "!=", "is not 23 or 42 or 43",
              "not 23,not 42,not 43")
          .add("23, not 42, not 43", "=", "is 23, and is not 42 or 43",
              "23,not 42,not 43")
          .add("23,NOT [30,40]", "=", "is 23, and is not in range [30, 40]",
              "23,not [30,40],not [30,40]")
          .add("23,NOT NULL", "=", "is 23, and is not null",
              "23,not null,not null")
          .add("23,NOT NULL,NOT NULL", "=", "is 23, and is not null",
              "23,not null,not null")
          .build();

  /** Runs a set of tests. */
  static <E> void forEach(Iterable<E> iterable, Consumer<E> consumer) {
    for (E e : iterable) {
      try {
        consumer.accept(e);
      } catch (AssertionError | RuntimeException x) {
        throw new RuntimeException("Failed '" + e + "'", x);
      }
    }
  }

  static class GrammarTestItem {
    final String expression;
    final String type;
    final @Nullable String low;
    final @Nullable String high;
    final @Nullable String bounds;
    final @Nullable String describe;
    final @Nullable String output;

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

    static ListBuilder builder() {
      return new ListBuilder();
    }

    @Override public String toString() {
      return expression;
    }

    /** Builds a list of {@link GrammarTestItem}. */
    public static class ListBuilder {
      final ImmutableList.Builder<GrammarTestItem> b = ImmutableList.builder();

      public ListBuilder add(String expression, String type, String describe,
          String output) {
        return add(expression, type, null, null, null, describe, output);
      }

      public ListBuilder add(String expression, String type, String low,
          String high, String bounds) {
        return add(expression, type, low, high, bounds, null, null);
      }

      public ListBuilder add(String expression, String type,
          @Nullable String low, @Nullable String high, @Nullable String bounds,
          @Nullable String describe, @Nullable String output) {
        b.add(
            new GrammarTestItem(expression, type, low, high, bounds, describe,
                output));
        return this;
      }

      public ImmutableList<GrammarTestItem> build() {
        return b.build();
      }
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

    public static ListBuilder builder() {
      return new ListBuilder();
    }

    @Override public String toString() {
      return expression + ":" + type + ":" + textInput;
    }

    /** Builds a list of {@link Triple}. */
    public static class ListBuilder {
      final ImmutableList.Builder<Triple> b = ImmutableList.builder();

      public ListBuilder add(String expression, String type, String textInput) {
        b.add(new Triple(expression, type, textInput));
        return this;
      }

      public ImmutableList<Triple> build() {
        return b.build();
      }
    }
  }

  static class Pair {
    final String expression;
    final String type;

    Pair(String expression, String type) {
      this.expression = expression;
      this.type = type;
    }

    public static ListBuilder builder() {
      return new ListBuilder();
    }

    @Override public String toString() {
      return expression + ":" + type;
    }
  }

  /** Builds a list of {@link Triple}. */
  public static class ListBuilder {
    final ImmutableList.Builder<Pair> b = ImmutableList.builder();

    public ListBuilder add(String expression, String type) {
      b.add(new Pair(expression, type));
      return this;
    }

    public ImmutableList<Pair> build() {
      return b.build();
    }
  }
}

// End TestValues.java
