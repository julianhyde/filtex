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
import net.hydromatic.filtex.ast.Asts;

import com.google.common.collect.ImmutableList;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static net.hydromatic.filtex.Filtex.parseFilterExpression;
import static net.hydromatic.filtex.ast.Asts.convertTypeToOption;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/** Tests number expressions. */
public class LocationTest {
  /** Runs a set of tests. */
  <E> void forEach(Iterable<E> iterable, Consumer<E> consumer) {
    for (E e : iterable) {
      try {
        consumer.accept(e);
      } catch (AssertionError | RuntimeException x) {
        throw new RuntimeException("Failed '" + e + "'", x);
      }
    }
  }

  void checkLocationItem(String expression, String expectedSummary,
      @Nullable String textInput) {
    final AstNode ast =
        parseFilterExpression(TypeFamily.LOCATION, expression);
    String summary =
        Filtex.summary(TypeFamily.LOCATION, expression, Locale.ENGLISH);
    assertThat(summary, is(expectedSummary));
    if (textInput != null) {
      assertThat(summary, is(textInput));
    }
  }

  @Test void testLocationGrammarCanParse() {
    forEach(TestValues.LOCATION_EXPRESSION_TEST_ITEMS, item ->
        checkLocationItem(item.expression, item.type, item.textInput));
  }

  /** Expressions that should fail. */
  static final List<String> FAIL_EXPRESSIONS =
      ImmutableList.of("(,)", "AND", "OR", "[inf,10]");

  @Test void testNumberGrammarCannotParse() {
    forEach(FAIL_EXPRESSIONS, expression -> {
      final AstNode ast =
          parseFilterExpression(TypeFamily.NUMBER, expression);
      assertThat(ast.model().type, is("matchesAdvanced"));
      assertThat(ast.expression(), is(expression));
    });
  }

  static final ImmutableList<TestValues.Triple> NUMERIC_CASES =
      TestValues.Triple.builder()
          .add("1", "=", "1")
          .add("1, 2, 3", "=", "1,2,3")
          .add("3.14159", "=", "3.14159")
          .add("123456789", "=", "123456789")
          .add("0.01", "=", "0.01")
          .add(".01", "=", "0.01")
          .add("-.01", "=", "-0.01")
          .add("-0.01", "=", "-0.01")
          .add("1, -1, 0.1", "=", "1,-1,0.1")

          .add("not 1", "!=", "1")
          .add("not 1, 2, 3", "!=", "1,2,3")
          .add("<> 1", "!=", "1")
          .add("!= 1, 2, 3", "!=", "1,2,3")
          .add("not -1.2", "!=", "-1.2")
          .add("not -.2", "!=", "-0.2")

          .add("> 1.1", ">", "1.1")
          .add(">0.1", ">", "0.1")
          .add(">999", ">", "999")
          .add("> -42", ">", "-42")
          .add(">-242", ">", "-242")
          .add(">    0", ">", "0")

          .add("< 1.1", "<", "1.1")
          .add("<3", "<", "3")
          .add("<0.1", "<", "0.1")
          .add("<999", "<", "999")
          .add("< -42", "<", "-42")
          .add("<-242", "<", "-242")
          .add("<    0", "<", "0")

          .add("<= 1.1", "<=", "1.1")
          .add("<=0.1", "<=", "0.1")
          .add("<=999", "<=", "999")
          .add("<= -42", "<=", "-42")
          .add("<=-242", "<=", "-242")
          .add("<=    0", "<=", "0")

          .add(">= 1.1", ">=", "1.1")
          .add(">=0.1", ">=", "0.1")
          .add(">=999", ">=", "999")
          .add(">= -42", ">=", "-42")
          .add(">=-242", ">=", "-242")
          .add(">=    0", ">=", "0")
          .build();

  void checkNumeric(String expression, String type, String textInput) {
    if (expression.equals("not 1, not 2")) {
      assert textInput.equals("1,2");
//      textInput = "1"; // FIXME
    }
    if (expression.equals("<> 1, <> 2")) {
      assert textInput.equals("1,2");
//      textInput = "1"; // FIXME
    }
    if (expression.equals("!= 1, != 2")) {
      assert textInput.equals("1,2");
//      textInput = "1"; // FIXME
    }
    final AstNode ast = parseFilterExpression(TypeFamily.NUMBER, expression);
    final List<Asts.Model> list = Asts.treeToList(ast);
    final Asts.Model item = list.get(0);
    String itemType = item.type;
    if (!type.equals("matchesAdvanced")) {
      itemType = convertTypeToOption(item.is, item.type);
    }
    assertThat(itemType, is(type));
    if (!type.equals("matchesAdvanced")
        && !type.equals("between")) {
      Object itemValue = item.valueString();
      assertThat(itemValue, is(textInput));
    }
  }

  @Test void testAdditionalNumberTests() {
    forEach(NUMERIC_CASES, c ->
        checkNumeric(c.expression, c.type, c.textInput));
  }

  static final List<TestValues.Pair> NULL_CASES =
      TestValues.Pair.builder()
          .add("NULL", "null")
          .add("NOT NULL", "!null")
          .add("null", "null")
          .add("not null", "!null")
          .add("nUll", "null")
          .add("Not Null", "!null")
          .build();

  void checkNull(String expression, String type) {
    final AstNode ast = parseFilterExpression(TypeFamily.NUMBER, expression);
    Asts.Model model = ast.model();
    final String itemType = convertTypeToOption(model.is, model.type);
    assertThat(itemType, is(type));
    assertThat(model.value, nullValue());
  }

  @Test void testNullValuesNumberTests() {
    forEach(NULL_CASES, pair -> checkNull(pair.expression, pair.type));
  }

  static final List<TestValues.GrammarTestItem> BETWEEN_CASES =
      TestValues.GrammarTestItem.builder()
          .add("1 to 5", "between", "1", "5", "[]")
          .add("-1.0 to .75", "between", "-1.0", ".75", "[]")
          .add(">7 AND <80.44", "between", "7", "80.44", "()")
          .add(">= 7 AND <80.44", "between", "7", "80.44", "[)")
          .add("<=80.44  AND    >.1", "between", "0.1", "80.44", "(]")
          .add("[2, 4]", "between", "2", "4", "[]")
          .add("[0.1,   -4)", "between", "0.1", "-4", "[)")
          .add("(0.1,   -4]", "between", "0.1", "-4", "(]")
          .add("(0.1, .11111)", "between", "0.1", "0.11111", "()")
          .add("NOT 1 to 5", "!between", "1", "5", "[]")
          .add("NOT -1.0 to .75", "!between", "-1.0", ".75", "[]")
          .add("not 3 to 80.44", "!between", "3", "80.44", "[]")
          .add("<7 OR >80.44", "!between", "7", "80.44", "()")
          .add("<= 7 OR >80.44", "!between", "7", "80.44", "[)")
          .add(">=80.44  OR    <.1", "!between", "0.1", "80.44", "(]")
          .add("NOT[2, 4]", "!between", "2", "4", "[]")
          .add("NOT [0.1,   -4)", "!between", "0.1", "-4", "[)")
          .add("NOT  (0.1,   -4]", "!between", "0.1", "-4", "(]")
          .add("NOT(0.1, .11111)", "!between", "0.1", "0.11111", "()")
          .build();

  @Test void testBetween() {
    forEach(BETWEEN_CASES, testItem -> {
      final AstNode ast =
          parseFilterExpression(TypeFamily.NUMBER, testItem.expression);
      Asts.Model model = ast.model();
      final String itemType = convertTypeToOption(model.is, model.type);
      assertThat(itemType, is(testItem.type));
      assertThat(model.low, is(testItem.low));
      assertThat(model.high, is(testItem.high));
      assertThat(model.bounds, is(testItem.bounds));
    });
  }

  static final List<TestValues.Triple> NOW_SUPPORTED_CASES =
      TestValues.Triple.builder()
          // the following previously had no deserializer,
          // but are now supported
          .add("1 to",                   ">=",     "1")
          .add("to -1",                  "<=",     "-1")
          .add("to 0.1",                 "<=",     "0.1")
          .add("not 1, not 2",           "!=",     "1,2")
          .add("<> 1, <> 2",             "!=",     "1,2")
          .add("!= 1, != 2",             "!=",     "1,2")
          .add("1, not 2",               "!=",     "1,2")
          .add(">1 AND <2 OR >3 AND <4", "between", ">1 AND <2 OR >3 AND <4")
          .build();

  @Test void testNowSupportedExpressions() {
    forEach(NOW_SUPPORTED_CASES, c ->
        checkNumeric(c.expression, c.type, c.textInput));
  }

  static final List<TestValues.Triple> UNSUPPORTED_CASES =
      TestValues.Triple.builder()
          .add("0.1.1.1", "matchesAdvanced",     "0.1.1.1")
          .add("0.....1", "matchesAdvanced",     "0.....1")
          .add("--1",     "matchesAdvanced",     "--1")
          .add("foo",     "matchesAdvanced",     "foo")
          .add("seventeen", "matchesAdvanced",     "seventeen")
          .add("&,,,$%testContext.#,,,$,testContext.",
              "matchesAdvanced",     "&,,,$%testContext.#,,,$,testContext.")
          .add("\\\\\\\\\\\\\\", "matchesAdvanced",
              "\\\\\\\\\\\\\\")
          .add("~`!testContext.#$%^*()-+=_{}[]|?",  "matchesAdvanced",
              "~`!testContext.#$%^*()-+=_{}[]|?")
          .add("<>,. ¡™£¢∞§¶•ªº–≠œ∑", "matchesAdvanced",
              "<>,. ¡™£¢∞§¶•ªº–≠œ∑")
          .add("´®†¥¨ˆøπ“‘åß∂ƒ©˙∆˚¬…æ", "matchesAdvanced",
              "´®†¥¨ˆøπ“‘åß∂ƒ©˙∆˚¬…æ")
          .add("Ω≈ç√∫˜µ≤≥÷", "matchesAdvanced", "Ω≈ç√∫˜µ≤≥÷")
          .add("😻🌚", "matchesAdvanced", "😻🌚")
          .add("^12345", "matchesAdvanced", "^12345")
          .add("1234^, 567", "matchesAdvanced", "1234^, 567")
          .build();

  @Test void testUnsupportedExpressions() {
    forEach(UNSUPPORTED_CASES, c ->
        checkNumeric(c.expression, c.type, c.textInput));
  }

}

// End ParserTest.java
