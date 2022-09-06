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
        .putIfInstance(prefix + "quarter.quarter", Ast.DateLiteral.class, node,
            d -> d.quarter)
        .putIfInstanceIf(prefix + "month", Ast.DateLiteral.class, node,
            d -> d.op != Op.ON, d -> d.month)
        .putIfInstanceIf(prefix + "date.month", Ast.DateLiteral.class, node,
            d -> d.op == Op.ON, d -> d.month)
        .putIfInstanceIf(prefix + "date.day", Ast.DateLiteral.class, node,
            d -> d.op == Op.ON, d -> d.day)
        .putIfInstance(prefix + "day", Ast.DayLiteral.class, node, n -> n.day)
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
            n -> "absolute")
        .putIfInstance(prefix + "intervalType", Ast.RelativeRange.class, node,
            Ast.RelativeRange::intervalType)
        .putIfInstance(prefix + "range", Ast.RelativeUnit.class, node,
            n -> "relative")
        .putIfInstance(prefix + "fromnow", Ast.RelativeUnit.class, node,
            n -> n.fromNow)
        .putIfInstance(prefix + "unit", Ast.RelativeUnit.class, node,
            n -> n.unit.singular)
        .putIfInstance(prefix + "value", Ast.RelativeUnit.class, node,
            n -> n.value)
        .putIfInstance(prefix + "unit", Ast.Past.class, node,
            n -> n.unit.singular)
        .putIfInstance(prefix + "value", Ast.Past.class, node, n -> n.value)
        .putIfInstanceIf(prefix + "complete", Ast.Past.class, node,
            n -> n.complete, n -> n.complete)
        .putIfInstance(prefix + "unit", Ast.Relative.class, node,
            n -> n.unit.singular)
        .putIfInstance(prefix + "value", Ast.Relative.class, node, n -> n.value)
        .putIfInstance(prefix + "unit", Ast.ThisUnit.class, node,
            n -> n.unit.singular)
        .putIfInstance(prefix + "startInterval", Ast.ThisRange.class, node,
            n -> n.startInterval.singular)
        .putIfInstance(prefix + "endInterval", Ast.ThisRange.class, node,
            n -> n.endInterval.singular)
        .putIfInstance(prefix + "unit", Ast.LastInterval.class, node,
            n -> n.unit.singular)
        .putIfInstance(prefix + "value", Ast.LastInterval.class, node,
            n -> n.value);
    if (node instanceof Ast.RangeInterval) {
      // TODO convert the following to new method .recurseIfInstance
      digest(digester, prefix + "end.", ((Ast.RangeInterval) node).end);
      digest(digester, prefix + "start.", ((Ast.RangeInterval) node).start);
    }
    if (node instanceof Ast.Range) {
      digest(digester, prefix + "end.", ((Ast.Range) node).end);
      digest(digester, prefix + "start.", ((Ast.Range) node).start);
    }
    if (node instanceof Ast.MonthInterval) {
      digest(digester, prefix + "end.", ((Ast.MonthInterval) node).end);
    }
    if (node instanceof Ast.Absolute) {
      digest(digester, prefix + "date.", ((Ast.Absolute) node).date);
    }
    if (node instanceof Ast.RelativeRange) {
      digest(digester, prefix + "endInterval.",
          ((Ast.RelativeRange) node).endInterval);
      digest(digester, prefix + "startInterval.",
          ((Ast.RelativeRange) node).startInterval);
    }
    return digester;
  }

  @Disabled
  @Test void testDateGrammarCanParse() {
    forEach(TestValues.DATE_EXPRESSION_TEST_ITEMS, i ->
        checkDateItem(i.expression, i.output, i.describe, i.type, i.digest));
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

  static final List<TestValues.Pair> BASIC_DATES =
      TestValues.Pair.builder()
          .add("this day", "{type=this, unit=day}")
          .add("this day to second",
              "{endInterval=second, startInterval=day, type=thisRange}")
          .add("this year to second",
              "{endInterval=second, startInterval=year, type=thisRange}")
          .add("this year to day",
              "{endInterval=day, startInterval=year, type=thisRange}")
          .add("3 days", "{type=past, unit=day, value=3}")
          .add("3 days ago", "{type=pastAgo, unit=day, value=3}")
          .add("3 months ago for 2 days",
              "{endInterval={type=interval, unit=day, value=2},"
                  + " intervalType=ago,"
                  + " startInterval={type=interval, unit=month, value=3},"
                  + " type=relative}")
          .add("before 3 days ago",
              "{fromnow=false, range=relative, type=before, unit=day, value=3}")
          .add("before 2018-01-01 12:00:00",
              "{date={day=1, hour=12, minute=0, month=1, second=0, year=2018},"
                  + " range=absolute, type=before}")
          .add("after 2018-10-05",
              "{date={day=5, month=10, year=2018}, range=absolute, type=after}")
          .add("2018-05-18 12:00:00 to 2018-05-18 14:00:00",
              "{end={day=18, hour=14, minute=0, month=5, second=0, year=2018}, "
                  + "start={day=18, hour=12, minute=0, month=5, second=0,"
                  + " year=2018}, type=range}")
          .add("2018-01-01 12:00:00 for 3 days",
              "{end={type=interval, unit=day, value=3}, "
                  + "start={day=1, hour=12, minute=0, month=1, second=0,"
                  + " year=2018}, type=rangeInterval}")
          .add("today", "{day=today, type=day}")
          .add("yesterday", "{day=yesterday, type=day}")
          .add("tomorrow", "{day=tomorrow, type=day}")
          .add("Monday", "{day=monday, type=day}")
          .add("next week", "{type=next, unit=week}")
          .add("3 days from now", "{type='from now', unit=day, value=3}")
          .add("3 days from now for 2 weeks",
              "{endInterval={type=interval, unit=week, value=2},"
                  + " intervalType='from now',"
                  + " startInterval={type=interval, unit=day, value=3},"
                  + " type=relative}")
          .build();

  void checkExpression(String expression, String expectedDigest) {
    final AstNode node = parseFilterExpression(TypeFamily.DATE, expression);
    final String digest = digest(node);
    assertThat(digest, is(expectedDigest));
    // TODO expect(summary('date', expression)).not.toBe('')
  }

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
          .add("FY2018-Q1",
              "{quarter={quarter=1}, type=fiscalQuarter, year=2018}")
          .build();

  @Test void testDateGrammarCanParseAbsoluteDate() {
    forEach(ABSOLUTE_DATES, pair ->
        checkExpression(pair.expression, pair.type));
  }

  static final List<TestValues.Pair> SECONDS =
      TestValues.Pair.builder()
          .add("1 second", "{type=past, unit=second, value=1}")
          .add("60 seconds", "{type=past, unit=second, value=60}")
          .add("60 seconds ago for 1 second",
              "{endInterval={type=interval, unit=second, value=1},"
                  + " intervalType=ago,"
                  + " startInterval={type=interval, unit=second, value=60},"
                  + " type=relative}")
          .build();

  @Test void testDateGrammarCanParseSeconds() {
    forEach(SECONDS, pair ->
        checkExpression(pair.expression, pair.type));
  }

  static final List<TestValues.Pair> MINUTES =
      TestValues.Pair.builder()
          .add("1 minute", "{type=past, unit=minute, value=1}")
          .add("60 minutes", "{type=past, unit=minute, value=60}")
          .add("60 minutes ago for 1 minute",
              "{endInterval={type=interval, unit=minute, value=1},"
                  + " intervalType=ago,"
                  + " startInterval={type=interval, unit=minute, value=60},"
                  + " type=relative}")
          .build();

  @Test void testDateGrammarCanParseMinutes() {
    forEach(MINUTES, pair ->
        checkExpression(pair.expression, pair.type));
  }

  static final List<TestValues.Pair> HOURS =
      TestValues.Pair.builder()
          .add("1 hour", "{type=past, unit=hour, value=1}")
          .add("24 hours", "{type=past, unit=hour, value=24}")
          .add("24 hours ago for 1 hour",
              "{endInterval={type=interval, unit=hour, value=1},"
                  + " intervalType=ago,"
                  + " startInterval={type=interval, unit=hour, value=24},"
                  + " type=relative}")
          .build();

  @Test void testDateGrammarCanParseHours() {
    forEach(HOURS, pair ->
        checkExpression(pair.expression, pair.type));
  }

  static final List<TestValues.Pair> DAYS =
      TestValues.Pair.builder()
          .add("today", "{day=today, type=day}")
          .add("2 days", "{type=past, unit=day, value=2}")
          .add("1 day ago", "{type=pastAgo, unit=day, value=1}")
          .add("7 days ago for 7 days",
              "{complete=true, type=past, unit=day, value=7}")
          .add("last 3 days", "{type=lastInterval, unit=day, value=3}")
          .add("7 days from now", "{type='from now', unit=day, value=7}")
          .build();

  @Test void testDateGrammarCanParseDays() {
    forEach(DAYS, pair ->
        checkExpression(pair.expression, pair.type));
  }

  static final List<TestValues.Pair> WEEKS =
      TestValues.Pair.builder()
          .add("1 week", "{type=past, unit=week, value=1}")
          .add("this week", "{type=this, unit=week}")
          .add("before this week", "{type=before_this, unit=week}")
          .add("after this week", "{type=after_this, unit=week}")
          .add("next week", "{type=next, unit=week}")
          .add("2 weeks", "{type=past, unit=week, value=2}")
          .add("2 weeks ago for 2 weeks",
              "{complete=true, type=past, unit=week, value=2}")
          .add("last week", "{type=last, unit=week}")
          .add("1 week ago", "{type=pastAgo, unit=week, value=1}")
          .build();

  @Test void testDateGrammarCanParseWeeks() {
    forEach(WEEKS, pair ->
        checkExpression(pair.expression, pair.type));
  }

  static final List<TestValues.Pair> MONTHS =
      TestValues.Pair.builder()
          .add("1 month", "{type=past, unit=month, value=1}")
          .add("this month", "{type=this, unit=month}")
          .add("2 months", "{type=past, unit=month, value=2}")
          .add("last month", "{type=last, unit=month}")
          .add("2 months ago", "{type=pastAgo, unit=month, value=2}")
          .add("2 months ago for 2 months",
              "{complete=true, type=past, unit=month, value=2}")
          .add("before 2 months ago",
              "{fromnow=false, range=relative, type=before, unit=month,"
                  + " value=2}")
          .add("before 2 months",
              "{fromnow=false, range=relative, type=before, unit=month,"
                  + " value=2}")
          .add("before 2 months from now",
              "{fromnow=true, range=relative, type=before, unit=month,"
                  + " value=2}")
          .add("next month", "{type=next, unit=month}")
          .add("2 months from now", "{type='from now', unit=month, value=2}")
          .add("6 months from now for 3 months",
              "{endInterval={type=interval, unit=month, value=3},"
                  + " intervalType='from now',"
                  + " startInterval={type=interval, unit=month, value=6},"
                  + " type=relative}")
          .build();

  @Test void testDateGrammarCanParseMonths() {
    forEach(MONTHS, pair ->
        checkExpression(pair.expression, pair.type));
  }

  static final List<TestValues.Pair> QUARTERS =
      TestValues.Pair.builder()
          .add("1 quarter", "{type=past, unit=quarter, value=1}")
          .add("this quarter", "{type=this, unit=quarter}")
          .add("2 quarters", "{type=past, unit=quarter, value=2}")
          .add("last quarter", "{type=last, unit=quarter}")
          .add("2 quarters ago", "{type=pastAgo, unit=quarter, value=2}")
          .add("before 2 quarters ago",
              "{fromnow=false, range=relative, type=before, unit=quarter,"
                  + " value=2}")
          .add("next quarter", "{type=next, unit=quarter}")
          .add("2018-07-01 for 1 quarter",
              "{end={type=interval, unit=quarter, value=1},"
                  + " start={day=1, month=7, year=2018}, type=rangeInterval}")
          .add("2018-Q4", "{quarter={quarter=4}, type=quarter, year=2018}")
          .build();

  @Test void testDateGrammarCanParseQuarters() {
    forEach(QUARTERS, pair ->
        checkExpression(pair.expression, pair.type));
  }

  static final List<TestValues.Pair> YEARS =
      TestValues.Pair.builder()
          .add("1 year", "{type=past, unit=year, value=1}")
          .add("this year", "{type=this, unit=year}")
          .add("next year", "{type=next, unit=year}")
          .add("2 years", "{type=past, unit=year, value=2}")
          .add("2 years ago for 2 years",
              "{complete=true, type=past, unit=year, value=2}")
          .add("last year", "{type=last, unit=year}")
          .add("2 years ago", "{type=pastAgo, unit=year, value=2}")
          .add("before 2 years ago",
              "{fromnow=false, range=relative, type=before, unit=year,"
                  + " value=2}")
          .build();

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
