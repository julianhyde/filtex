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
import net.hydromatic.filtex.ast.Asts;
import net.hydromatic.filtex.ast.Date;
import net.hydromatic.filtex.ast.Datetime;
import net.hydromatic.filtex.ast.Op;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static net.hydromatic.filtex.Filtex.parseFilterExpression;
import static net.hydromatic.filtex.TestValues.forEach;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/** Tests date expressions. */
public class DateTest {

  private String digest(AstNode node) {
    return digest(new LocationTest.Digester(), "", node)
        .toString();
  }

  private LocationTest.Digester digest(LocationTest.Digester digester,
      String prefix, Object node) {
    digester
        .putIfInstance(prefix + "type", AstNode.class, node, AstNode::type)
        .putIfInstanceIf(prefix + "year", Ast.DateLiteral.class, node,
            d -> d.op != Op.ON, d -> d.year)
        .putIfInstanceIf(prefix + "date.year", Ast.DateLiteral.class, node,
            d -> d.op == Op.ON, d -> d.year)
        .putIfInstance(prefix + "quarter", Ast.DateLiteral.class, node,
            d -> d.quarter)
        .putIfInstanceIf(prefix + "month", Ast.DateLiteral.class, node,
            d -> d.op != Op.ON, d -> d.month)
        .putIfInstanceIf(prefix + "date.month", Ast.DateLiteral.class, node,
            d -> d.op == Op.ON, d -> d.month)
        .putIfInstanceIf(prefix + "date.day", Ast.DateLiteral.class, node,
            d -> d.op == Op.ON, d -> d.day)
        .putIfInstance(prefix + "unit", Ast.Interval.class, node, i ->
            i.unit.singular)
        .putIfInstance(prefix + "value", Ast.Interval.class, node, i -> i.value)
        .putIfInstance(prefix + "year", Ast.MonthInterval.class, node,
            d -> d.year)
        .putIfInstance(prefix + "month", Ast.MonthInterval.class, node,
            d -> d.month)
        .putIfInstance(prefix + "year", Date.class, node, d -> d.year)
        .putIfInstance(prefix + "month", Date.class, node, d -> d.month)
        .putIfInstance(prefix + "day", Date.class, node, d -> d.day)
        .putIfInstance(prefix + "hour", Datetime.class, node, d -> d.hour)
        .putIfInstance(prefix + "minute", Datetime.class, node, d -> d.minute)
        .putIfInstance(prefix + "second", Datetime.class, node, d -> d.second)
        .putIfInstance(prefix + "range", Ast.Absolute.class, node,
            n -> "absolute");
    if (node instanceof Ast.RangeInterval) {
      // TODO convert the following to new method .recurseIfInstance
      digest(digester, prefix + "end.", ((Ast.RangeInterval) node).end);
      digest(digester, prefix + "start.", ((Ast.RangeInterval) node).start);
    }
    if (node instanceof Ast.MonthInterval) {
      digest(digester, prefix + "end.", ((Ast.MonthInterval) node).end);
    }
    if (node instanceof Ast.Absolute) {
      digest(digester, prefix + "date.", ((Ast.Absolute) node).date);
    }
    return digester;
  }

  @Disabled
  @Test void testLocationGrammarCanParse() {
    forEach(TestValues.DATE_EXPRESSION_TEST_ITEMS, item ->
        checkDateItem(item.expression, item.output, item.describe, item.type,
            item.digest));
  }

  void checkDateItem(String expression, String expectedOutput,
      String expectedSummary, @Nullable String expectedType,
      String expectedDigest) {
    // test ast
    final AstNode ast =
        parseFilterExpression(TypeFamily.DATE, expression);
    assertThat(digest(ast), is(expectedDigest));

    // test descriptions
    String summary =
        Filtex.summary(TypeFamily.DATE, expression, Locale.ENGLISH);

    assertThat(summary, is(expectedSummary));
    if (expectedDigest != null) {
      final String digest = digest(ast);
      assertThat(digest, is(expectedDigest));
    }

    // test item type
    final List<AstNode> list = Asts.treeToList(ast);
    final AstNode item = list.get(0);
    if (expectedType != null) {
      assertThat(item.type(), is(expectedType));
    }

    // test serialized output
    // some filter types can't be represented by DateFilter,
    // we expect this to be parsed as `type` above,
    // but be converted to `matchesAdvanced`
    final String dateComponentType =
        Asts.convertTypeToMatchesAdvancedOption(item.type());
    final String dateOutput =
          dateComponentType.equals("matchesAdvanced")
              ? expression
              : Asts.dateFilterToString(ast, TypeFamily.DATE);
    assertThat(dateOutput, is(expectedOutput));
  }

  @Disabled
  @Test void testDateGrammarCanParse() {
    forEach(TestValues.DATE_EXPRESSION_TEST_ITEMS, i ->
        checkDateItem(i.expression, i.output, i.describe, i.type, i.digest));
  }

  static final List<TestValues.Pair> BASIC_DATES =
      TestValues.Pair.builder()
          .add("this day", "xx")
          .add("this day to second", "xx")
          .add("this year to second", "xx")
          .add("this year to day", "xx")
          .add("3 days", "xx")
          .add("3 days ago", "xx")
          .add("3 months ago for 2 days", "xx")
          .add("before 3 days ago", "xx")
          .add("before 2018-01-01 12:00:00", "xx")
          .add("after 2018-10-05", "xx")
          .add("2018-05-18 12:00:00 to 2018-05-18 14:00:00", "xx")
          .add("2018-01-01 12:00:00 for 3 days", "xx")
          .add("today", "xx")
          .add("yesterday", "xx")
          .add("tomorrow", "xx")
          .add("Monday", "xx")
          .add("next week", "xx")
          .add("3 days from now", "xx")
          .add("3 days from now for 2 weeks", "xx")
          .build();

  void checkExpression(String expression, String expectedDigest) {
    final AstNode node = parseFilterExpression(TypeFamily.DATE, expression);
    final String digest = digest(node);
    assertThat(digest, is(expectedDigest));
    // TODO expect(summary('date', expression)).not.toBe('')
  }

  @Disabled
  @Test void testDateGrammarCanParseBasicDate() {
    forEach(BASIC_DATES, pair ->
        checkExpression(pair.expression, pair.type));
  }

  static final List<TestValues.Pair> ABSOLUTE_DATES =
      TestValues.Pair.builder()
          .add("2018/05/29", "{date={day=29, month=5, year=2018}, type=on}")
          .add("2018/05/10 for 3 days",
              "{end={type=interval, unit=day, value=3}, "
                  + "start={day=10, month=5, year=2018}, type=rangeInterval}")
          .add("after 2018/05/10",
              "{date={day=10, month=5, year=2018}, range=absolute,"
                  + " type=after}")
          .add("before 2018/05/10",
              "{date={day=10, month=5, year=2018}, range=absolute,"
                  + " type=before}")
          .add("2018/05", "{month=5, type=month, year=2018}")
          .add("2018/05 for 2 months",
              "{end={type=interval, unit=month, value=2}, month=5,"
                  + " type=monthInterval, year=2018}")
          .add("2018/05/10 05:00 for 5 hours",
              "{end={type=interval, unit=hour, value=5},"
                  + " start={day=10, hour=5, minute=0, month=5, year=2018},"
                  + " type=rangeInterval}")
          .add("2018/05/10 for 5 months",
              "{end={type=interval, unit=month, value=5},"
                  + " start={day=10, month=5, year=2018}, type=rangeInterval}")
          .add("2018", "{type=year, year=2018}")
          .add("FY2018", "{type=fiscalYear, year=2018}")
          .add("FY2018-Q1", "{quarter=Q1, type=fiscalQuarter, year=2018}")
          .build();

  @Test void testDateGrammarCanParseAbsoluteDate() {
    forEach(ABSOLUTE_DATES, pair ->
        checkExpression(pair.expression, pair.type));
  }

  static final List<TestValues.Pair> SECONDS =
      TestValues.Pair.builder()
          .add("1 second", "xx")
          .add("60 seconds", "xx")
          .add("60 seconds ago for 1 second", "xx")
          .build();

  @Disabled
  @Test void testDateGrammarCanParseSeconds() {
    forEach(SECONDS, pair ->
        checkExpression(pair.expression, pair.type));
  }

  static final List<TestValues.Pair> MINUTES =
      TestValues.Pair.builder()
          .add("1 minute", "xx")
          .add("60 minutes", "xx")
          .add("60 minutes ago for 1 minute", "xx")
          .build();

  @Disabled
  @Test void testDateGrammarCanParseMinutes() {
    forEach(MINUTES, pair ->
        checkExpression(pair.expression, pair.type));
  }

  static final List<TestValues.Pair> HOURS =
      TestValues.Pair.builder()
          .add("1 hour", "xx")
          .add("24 hours", "xx")
          .add("24 hours ago for 1 hour", "xx")
          .build();

  @Disabled
  @Test void testDateGrammarCanParseHours() {
    forEach(HOURS, pair ->
        checkExpression(pair.expression, pair.type));
  }

  static final List<TestValues.Pair> DAYS =
      TestValues.Pair.builder()
          .add("today", "xx")
          .add("2 days", "xx")
          .add("1 day ago", "xx")
          .add("7 days ago for 7 days", "xx")
          .add("last 3 days", "xx")
          .add("7 days from now", "xx")
          .build();

  @Disabled
  @Test void testDateGrammarCanParseDays() {
    forEach(DAYS, pair ->
        checkExpression(pair.expression, pair.type));
  }

  static final List<TestValues.Pair> WEEKS =
      TestValues.Pair.builder()
          .add("1 week", "xx")
          .add("this week", "xx")
          .add("before this week", "xx")
          .add("after this week", "xx")
          .add("next week", "xx")
          .add("2 weeks", "xx")
          .add("2 weeks ago for 2 weeks", "xx")
          .add("last week", "xx")
          .add("1 week ago", "xx")
          .build();

  @Disabled
  @Test void testDateGrammarCanParseWeeks() {
    forEach(WEEKS, pair ->
        checkExpression(pair.expression, pair.type));
  }

  static final List<TestValues.Pair> MONTHS =
      TestValues.Pair.builder()
          .add("1 month", "xx")
          .add("this month", "xx")
          .add("2 months", "xx")
          .add("last month", "xx")
          .add("2 months ago", "xx")
          .add("2 months ago for 2 months", "xx")
          .add("before 2 months ago", "xx")
          .add("before 2 months", "xx")
          .add("before 2 months from now", "xx")
          .add("next month", "xx")
          .add("2 months from now", "xx")
          .add("6 months from now for 3 months", "xx")
          .build();

  @Disabled
  @Test void testDateGrammarCanParseMonths() {
    forEach(MONTHS, pair ->
        checkExpression(pair.expression, pair.type));
  }

  static final List<TestValues.Pair> QUARTERS =
      TestValues.Pair.builder()
          .add("1 quarter", "xx")
          .add("this quarter", "xx")
          .add("2 quarters", "xx")
          .add("last quarter", "xx")
          .add("2 quarters ago", "xx")
          .add("before 2 quarters ago", "xx")
          .add("next quarter", "xx")
          .add("2018-07-01 for 1 quarter", "xx")
          .add("2018-Q4", "xx")
          .build();

  @Disabled
  @Test void testDateGrammarCanParseQuarters() {
    forEach(QUARTERS, pair ->
        checkExpression(pair.expression, pair.type));
  }

  static final List<TestValues.Pair> YEARS =
      TestValues.Pair.builder()
          .add("1 year", "xx")
          .add("this year", "xx")
          .add("next year", "xx")
          .add("2 years", "xx")
          .add("2 years ago for 2 years", "xx")
          .add("last year", "xx")
          .add("2 years ago", "xx")
          .add("before 2 years ago", "xx")
          .build();

  @Disabled
  @Test void testDateGrammarCanParseYears() {
    forEach(YEARS, pair ->
        checkExpression(pair.expression, pair.type));
  }

  static final List<TestValues.Pair> MULTIPLE_CLAUSE_EXPRESSIONS =
      TestValues.Pair.builder()
          .add("1 year ago, 1 month ago", "xx")
          .build();

  @Disabled
  @Test void testDateGrammarWithMultipleClauses() {
    forEach(MULTIPLE_CLAUSE_EXPRESSIONS, pair ->
        checkExpression(pair.expression, pair.type));
  }

  static final List<TestValues.Pair> INVALID_DATES =
      TestValues.Pair.builder()
          .add("-1", "xx")
          .add("not a valid date", "xx")
          .build();

  @Disabled
  @Test void testDateGrammarInvalidDatesShowAsMatchesAdvancedType() {
    forEach(INVALID_DATES, pair ->
        checkExpression(pair.expression, pair.type));
  }
}

// End DateTest.java
