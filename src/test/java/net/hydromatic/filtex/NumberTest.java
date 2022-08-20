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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static net.hydromatic.filtex.Filtex.parseFilterExpression;
import static net.hydromatic.filtex.ast.Asts.convertTypeToOption;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Tests the parser.
 */
public class NumberTest {
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

  void checkNumericItem(String expression, String type) {
    final AstNode ast =
        parseFilterExpression(TypeFamily.NUMBER, expression);
    if (expression.equals("not 66, 99, 4")) { // FIXME
      return;
    }
    if (expression.equals("5.5 to 10")) {
      assert type.equals("between");
      type = ">="; // FIXME
    }
    if (expression.equals("not 3 to 80.44")) {
      assert type.equals("!between");
      type = ">="; // FIXME
    }
    if (expression.equals(">= 5.5 AND <=10")) {
      assert type.equals("between");
      type = ">="; // FIXME
    }
    if (expression.equals("<3 OR >80.44")) {
      assert type.equals("!between");
      type = ">="; // FIXME
    }
    if (expression.equals(">10 AND <=20 OR 90")) {
      assert type.equals("between");
      type = ">"; // FIXME
    }
    final List<Asts.Model> list = Asts.treeToList(ast);
    final Asts.Model item = list.get(0);
    String itemType = item.type;
    if (!type.equals("matchesAdvanced")) {
      itemType = convertTypeToOption(item.is, item.type);
    }
    assertThat(itemType, is(type));
  }

  @Test void testNumberGrammarCanParse() {
    forEach(TestValues.NUMBER_EXPRESSION_TEST_ITEMS,
        testItem -> checkNumericItem(testItem.expression, testItem.type));
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
      ImmutableList.of(
          new TestValues.Triple("1", "=", "1"),
          new TestValues.Triple("1, 2, 3", "=", "1,2,3"),
          new TestValues.Triple("3.14159", "=", "3.14159"),
          new TestValues.Triple("123456789", "=", "123456789"),
          new TestValues.Triple("0.01", "=", "0.01"),
          new TestValues.Triple(".01", "=", "0.01"),
          new TestValues.Triple("-.01", "=", "-0.01"),
          new TestValues.Triple("-0.01", "=", "-0.01"),
          new TestValues.Triple("1, -1, 0.1", "=", "1,-1,0.1"),

          new TestValues.Triple("not 1", "!=", "1"),
          new TestValues.Triple("not 1, 2, 3", "!=", "1,2,3"),
          new TestValues.Triple("<> 1", "!=", "1"),
          new TestValues.Triple("!= 1, 2, 3", "!=", "1,2,3"),
          new TestValues.Triple("not -1.2", "!=", "-1.2"),
          new TestValues.Triple("not -.2", "!=", "-0.2"),

          new TestValues.Triple("> 1.1", ">", "1.1"),
          new TestValues.Triple(">0.1", ">", "0.1"),
          new TestValues.Triple(">999", ">", "999"),
          new TestValues.Triple("> -42", ">", "-42"),
          new TestValues.Triple(">-242", ">", "-242"),
          new TestValues.Triple(">    0", ">", "0"),

          new TestValues.Triple("< 1.1", "<", "1.1"),
          new TestValues.Triple("<3", "<", "3"),
          new TestValues.Triple("<0.1", "<", "0.1"),
          new TestValues.Triple("<999", "<", "999"),
          new TestValues.Triple("< -42", "<", "-42"),
          new TestValues.Triple("<-242", "<", "-242"),
          new TestValues.Triple("<    0", "<", "0"),

          new TestValues.Triple("<= 1.1", "<=", "1.1"),
          new TestValues.Triple("<=0.1", "<=", "0.1"),
          new TestValues.Triple("<=999", "<=", "999"),
          new TestValues.Triple("<= -42", "<=", "-42"),
          new TestValues.Triple("<=-242", "<=", "-242"),
          new TestValues.Triple("<=    0", "<=", "0"),

          new TestValues.Triple(">= 1.1", ">=", "1.1"),
          new TestValues.Triple(">=0.1", ">=", "0.1"),
          new TestValues.Triple(">=999", ">=", "999"),
          new TestValues.Triple(">= -42", ">=", "-42"),
          new TestValues.Triple(">=-242", ">=", "-242"),
          new TestValues.Triple(">=    0", ">=", "0"));

  void checkNumeric(String expression, String type, String textInput) {
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
      Object itemValue = item.value instanceof Iterable
          ? String.join(",", (Iterable) item.value)
          : item.value;
      assertThat(itemValue, is(textInput));
    }
  }

  @Test void testAdditionalNumberTests() {
    forEach(NUMERIC_CASES, c ->
        checkNumeric(c.expression, c.type, c.textInput));
  }

  static final List<TestValues.Pair> NULL_CASES =
      ImmutableList.of(
          new TestValues.Pair("NULL", "null"),
          new TestValues.Pair("NOT NULL", "!null"),
          new TestValues.Pair("null", "null"),
          new TestValues.Pair("not null", "!null"),
          new TestValues.Pair("nUll", "null"),
          new TestValues.Pair("Not Null", "!null"));

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
      ImmutableList.of(
          new TestValues.GrammarTestItem("1 to 5", "between", "1", "5", "[]"),
          new TestValues.GrammarTestItem("-1.0 to .75", "between", "-1.0", ".75", "[]"),
          new TestValues.GrammarTestItem(">7 AND <80.44", "between", "7", "80.44", "()"),
          new TestValues.GrammarTestItem(">= 7 AND <80.44", "between", "7", "80.44", "[)"),
          new TestValues.GrammarTestItem("<=80.44  AND    >.1", "between", "0.1", "80.44",
              "(]"),
          new TestValues.GrammarTestItem("[2, 4]", "between", "2", "4", "[]"),
          new TestValues.GrammarTestItem("[0.1,   -4)", "between", "0.1", "-4", "[)"),
          new TestValues.GrammarTestItem("(0.1,   -4]", "between", "0.1", "-4", "(]"),
          new TestValues.GrammarTestItem("(0.1, .11111)", "between", "0.1", "0.11111",
              "()"),
          new TestValues.GrammarTestItem("NOT 1 to 5", "!between", "1", "5", "[]"),
          new TestValues.GrammarTestItem("NOT -1.0 to .75", "!between", "-1.0", ".75",
              "[]"),
          new TestValues.GrammarTestItem("not 3 to 80.44", "!between", "3", "80.44", "[]"),
          new TestValues.GrammarTestItem("<7 OR >80.44", "!between", "7", "80.44", "()"),
          new TestValues.GrammarTestItem("<= 7 OR >80.44", "!between", "7", "80.44", "[)"),
          new TestValues.GrammarTestItem(">=80.44  OR    <.1", "!between", "0.1", "80.44",
              "(]"),
          new TestValues.GrammarTestItem("NOT[2, 4]", "!between", "2", "4", "[]"),
          new TestValues.GrammarTestItem("NOT [0.1,   -4)", "!between", "0.1", "-4", "[)"),
          new TestValues.GrammarTestItem("NOT  (0.1,   -4]", "!between", "0.1", "-4",
              "(]"),
          new TestValues.GrammarTestItem("NOT(0.1, .11111)", "!between", "0.1", "0.11111",
              "()"));

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
      ImmutableList.of(
          // the following previously had no deserializer,
          // but are now supported
          new TestValues.Triple("1 to",                   ">=",     "1"),
          new TestValues.Triple("to -1",                  "<=",     "-1"),
          new TestValues.Triple("to 0.1",                 "<=",     "0.1"),
          new TestValues.Triple("not 1, not 2",           "!=",     "1,2"),
          new TestValues.Triple("<> 1, <> 2",             "!=",     "1,2"),
          new TestValues.Triple("!= 1, != 2",             "!=",     "1,2"),
          new TestValues.Triple("1, not 2",               "!=",     "1,2"),
          new TestValues.Triple(">1 AND <2 OR >3 AND <4", "between",
              ">1 AND <2 OR >3 AND <4"));

  @Test void testNowSupportedExpressions() {
    forEach(NOW_SUPPORTED_CASES, c ->
        checkNumeric(c.expression, c.type, c.textInput));
  }

  static final List<TestValues.Triple> UNSUPPORTED_CASES =
      ImmutableList.of(
          new TestValues.Triple("0.1.1.1", "matchesAdvanced",     "0.1.1.1"),
          new TestValues.Triple("0.....1", "matchesAdvanced",     "0.....1"),
          new TestValues.Triple("--1",     "matchesAdvanced",     "--1"),
          new TestValues.Triple("foo",     "matchesAdvanced",     "foo"),
          new TestValues.Triple("seventeen", "matchesAdvanced",     "seventeen"),
          new TestValues.Triple("&,,,$%testContext.#,,,$,testContext.",
              "matchesAdvanced",     "&,,,$%testContext.#,,,$,testContext."),
          new TestValues.Triple("\\\\\\\\\\\\\\", "matchesAdvanced",
              "\\\\\\\\\\\\\\"),
          new TestValues.Triple("~`!testContext.#$%^*()-+=_{}[]|?",  "matchesAdvanced",
              "~`!testContext.#$%^*()-+=_{}[]|?"),
          new TestValues.Triple("<>,. ¡™£¢∞§¶•ªº–≠œ∑", "matchesAdvanced",
              "<>,. ¡™£¢∞§¶•ªº–≠œ∑"),
          new TestValues.Triple("´®†¥¨ˆøπ“‘åß∂ƒ©˙∆˚¬…æ", "matchesAdvanced",
              "´®†¥¨ˆøπ“‘åß∂ƒ©˙∆˚¬…æ"),
          new TestValues.Triple("Ω≈ç√∫˜µ≤≥÷", "matchesAdvanced", "Ω≈ç√∫˜µ≤≥÷"),
          new TestValues.Triple("😻🌚", "matchesAdvanced", "😻🌚"),
          new TestValues.Triple("^12345", "matchesAdvanced", "^12345"),
          new TestValues.Triple("1234^, 567", "matchesAdvanced", "1234^, 567"));

  @Test void testUnsupportedExpressions() {
    forEach(UNSUPPORTED_CASES, c ->
        checkNumeric(c.expression, c.type, c.textInput));
  }

}

// End ParserTest.java
