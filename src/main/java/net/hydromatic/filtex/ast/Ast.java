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

import com.google.common.collect.ImmutableList;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

import static java.util.Objects.requireNonNull;

/** Various subclasses of AST nodes. */
public class Ast {
  private Ast() {
  }

  /** Date literal. */
  public static class DateLiteral extends AstNode {
    public final int year;
    public final @Nullable Integer quarter;
    public final @Nullable Integer month;
    public final @Nullable Integer day;
    private final @Nullable Integer hour;
    private final @Nullable Integer minute;
    private final @Nullable Integer second;

    public DateLiteral(Op op, int year, @Nullable Integer quarter,
        @Nullable Integer month, @Nullable Integer day, @Nullable Integer hour,
        @Nullable Integer minute, @Nullable Integer second) {
      super(Pos.ZERO, op);
      this.year = year;
      this.quarter = quarter;
      this.month = month;
      this.day = day;
      this.hour = hour;
      this.minute = minute;
      this.second = second;
    }

    @Override public Digester digest(Digester digester) {
      return super.digest(digester)
          .put(op == Op.ON ? "date.year" : "year", year)
          .putIf(op == Op.ON ? "date.month" : "month", month)
          .putIf(op == Op.ON ? "date.day" : "day", day)
          .putIf("quarter.quarter", quarter);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      throw new AssertionError();
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
      throw new AssertionError();
    }
  }

  /** Day literal. */
  public static class DayLiteral extends AstNode {
    public final String day;

    public DayLiteral(String day) {
      super(Pos.ZERO, Op.DAY);
      this.day = day;
    }

    @Override public Digester digest(Digester digester) {
      return super.digest(digester)
          .put("day", day);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      throw new AssertionError();
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
      throw new AssertionError();
    }
  }

  /** Interval, e.g. "3 days". */
  public static class Interval extends AstNode {
    public final BigDecimal value;
    public final DatetimeUnit unit;

    Interval(DatetimeUnit unit, BigDecimal value) {
      super(Pos.ZERO, Op.INTERVAL);
      this.unit = unit;
      this.value = value;
    }

    @Override public boolean equals(Object o) {
      return o == this
          || o instanceof Interval
          && value.equals(((Interval) o).value)
          && unit == ((Interval) o).unit;
    }

    @Override public int hashCode() {
      return Objects.hash(value, unit);
    }

    @Override public Digester digest(Digester digester) {
      return super.digest(digester)
          .put("unit", unit.singular)
          .put("value", value);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      throw new AssertionError();
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
      throw new AssertionError();
    }
  }

  /** Date range, e.g. "2018/05/10 to 2018/05/13". */
  public static class Range extends AstNode {
    public final Date start;
    public final Date end;

    Range(Date start, Date end) {
      super(Pos.ZERO, Op.RANGE);
      this.start = start;
      this.end = end;
    }

    @Override public Digester digest(Digester digester) {
      return super.digest(digester)
          .sub("end", end)
          .sub("start", start);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      throw new AssertionError();
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
      throw new AssertionError();
    }
  }

  /** Date range based on interval, e.g. "2018/05/10 for 3 days". */
  public static class RangeInterval extends AstNode {
    public final Date start;
    public final Ast.Interval end;

    RangeInterval(Date start, Ast.Interval end) {
      super(Pos.ZERO, Op.RANGE_INTERVAL);
      this.start = start;
      this.end = end;
    }

    @Override public Digester digest(Digester digester) {
      return super.digest(digester)
          .sub("end", end)
          .sub("start", start);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      throw new AssertionError();
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
      throw new AssertionError();
    }
  }

  /** Month range, e.g. "2018/05 for 3 months". */
  public static class MonthInterval extends AstNode {
    public final int year;
    public final int month;
    public final Ast.Interval end;

    MonthInterval(int year, int month, Ast.Interval end) {
      super(Pos.ZERO, Op.MONTH_INTERVAL);
      this.year = year;
      this.month = month;
      this.end = end;
    }

    @Override public Digester digest(Digester digester) {
      return super.digest(digester)
          .sub("end", end)
          .put("year", year)
          .put("month", month);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      throw new AssertionError();
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
      throw new AssertionError();
    }
  }

  /** Geographical box. */
  public static class Point extends AstNode {
    public final Location location;

    protected Point(Location location) {
      super(Pos.ZERO, Op.POINT);
      this.location = location;
    }

    @Override public Digester digest(Digester digester) {
      return super.digest(digester)
          .put("lat", location.latitude)
          .put("long", location.longitude);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      return writer.append(location.latitude.toString())
          .append(", ").append(location.longitude.toString());
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
    }
  }

  /** Geographical box. */
  public static class Box extends AstNode {
    public final Location from;
    public final Location to;

    protected Box(Location from, Location to) {
      super(Pos.ZERO, Op.BOX);
      this.from = from;
      this.to = to;
    }

    @Override public Digester digest(Digester digester) {
      return super.digest(digester)
          .put("lat", from.latitude)
          .put("lon", from.longitude)
          .put("lat1", to.latitude)
          .put("lon1", to.longitude);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      appendDegrees(writer, from.latitude, "°S", "°N");
      writer.append(", ");
      appendDegrees(writer, from.longitude, "°W", "°E");
      writer.append(" to ");
      appendDegrees(writer, to.latitude, "°S", "°N");
      writer.append(", ");
      appendDegrees(writer, to.longitude, "°W", "°E");
      return writer;
    }

    private void appendDegrees(AstWriter w, BigDecimal v, String neg,
        String pos) {
      v = v.setScale(1, RoundingMode.HALF_EVEN);
      if (v.signum() < 0) {
        w.appendLiteral(v.negate()).append(neg);
      } else {
        w.appendLiteral(v).append(pos);
      }
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
    }
  }

  /** Geographical circle. */
  public static class Circle extends AstNode {
    public final BigDecimal distance;
    public final Unit unit;
    public final Location location;

    Circle(BigDecimal distance, Unit unit, Location location) {
      super(Pos.ZERO, Op.CIRCLE);
      this.distance = distance;
      this.unit = unit;
      this.location = location;
    }

    @Override public Digester digest(Digester digester) {
      return super.digest(digester)
          .put("distance", distance)
          .put("unit", unit.plural)
          .put("lat", location.latitude)
          .put("lon", location.longitude);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      return writer.appendLiteral(distance)
          .append(" ").append(unit.plural)
          .append(" from ").appendLiteral(location.latitude)
          .append(", ").appendLiteral(location.longitude);
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
    }
  }

  /** Numeric comparison. */
  @SuppressWarnings("rawtypes")
  public static class Comparison extends AstNode {
    public final boolean is;
    public final List<Comparable> value;

    Comparison(boolean is, Op op, List<Comparable> value) {
      super(Pos.ZERO, op);
      this.is = is;
      this.value = value;
    }

    @Override public boolean is() {
      return is;
    }

    @Override public Iterable<Comparable> value() {
      return value;
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
      visitor.visit(this, parent);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      return writer.append(valueString());
    }
  }

  /** Call with zero arguments, optionally negated. */
  public static class Call0 extends AstNode {
    final boolean is;

    Call0(Op op, boolean is) {
      super(Pos.ZERO, op);
      this.is = is;
    }

    @Override public boolean is() {
      return is;
    }

    @Override public AstWriter unparse(AstWriter writer) {
      switch (op) {
      case NULL:
        return writer.append(is ? "is null" : "is not null");
      case NOTNULL:
        return writer.append("is not null");
      default:
        if (!is) {
          writer.append("not ");
        }
        return writer.append(op.s);
      }
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
      visitor.visit(this, parent);
    }
  }

  /** Call with one argument, optionally negated. */
  public static class Call1 extends AstNode {
    public final boolean is;
    public final AstNode node;

    Call1(Op op, boolean is, AstNode node) {
      super(Pos.ZERO, op);
      this.is = is;
      this.node = node;
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
      visitor.visit(this, parent);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      writer.append("(");
      if (!is) {
        writer.append("not ");
      }
      writer.append(op.s);
      return writer.append(")");
    }
  }

  /** Call with two arguments, optionally negated. */
  public static class Call2 extends AstNode {
    public final AstNode left;
    public AstNode right;

    public Call2(Op op, AstNode left, AstNode right) {
      super(Pos.ZERO, op);
      this.left = left;
      this.right = right;
    }

    @Override public AstWriter unparse(AstWriter writer) {
      return writer.infix(left, op, right);
    }

    @Override public void accept(AstVisitor visitor, AstNode parent) {
      visitor.visit(this, parent);
    }
  }

  /** Numeric range. */
  public static class NumericRange extends AstNode {
    public final boolean is;
    public final BigDecimal left;
    public final BigDecimal right;

    NumericRange(Op op, String id, boolean is, BigDecimal left,
        BigDecimal right) {
      super(Pos.ZERO, op);
      this.is = is;
      this.left = requireNonNull(left);
      this.right = requireNonNull(right);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      writer.append(op.left);
      if (left != null) {
        writer.appendLiteral(left);
      }
      writer.append(",");
      if (right != null) {
        writer.appendLiteral(right);
      }
      writer.append(op.right);
      return writer;
    }

    @Override public void accept(AstVisitor visitor, AstNode parent) {
      visitor.visit(this, parent);
    }

    @Override public String type() {
      switch (op) {
      case ABSENT_CLOSED:
      case ABSENT_OPEN:
      case CLOSED_ABSENT:
      case OPEN_ABSENT:
        return op.s;
      default:
        return "between";
      }
    }

    @Override public boolean is() {
      return is;
    }

    @Override public Iterable<Comparable> value() {
      return left != null && right != null
          ? ImmutableList.of(left, right)
          : left != null
              ? ImmutableList.of(left)
              : ImmutableList.of(right);
    }

    @Override public String low() {
      return left == null ? "" : left.toString();
    }

    @Override public String high() {
      return right == null ? "" : right.toString();
    }

    @Override public String bounds() {
      return op.s;
    }
  }

  /** MatchesAdvanced. */
  @SuppressWarnings("rawtypes")
  public static class MatchesAdvanced extends AstNode {
    public final String expression;

    MatchesAdvanced(String expression) {
      super(Pos.ZERO, Op.MATCHES_ADVANCED);
      this.expression = expression;
    }

    @Override public String expression() {
      return expression;
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
      visitor.visit(this, parent);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      return writer.append(expression);
    }
  }

  public static class Absolute extends AstNode {
    public final Date date;

    Absolute(Date date, boolean before) {
      super(Pos.ZERO, before ? Op.BEFORE : Op.AFTER);
      this.date = date;
    }

    @Override public Digester digest(Digester digester) {
      return super.digest(digester)
          .sub("date", date)
          .put("range", "absolute");
    }

    @Override public AstWriter unparse(AstWriter writer) {
      throw new AssertionError();
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
      throw new AssertionError();
    }
  }

  public static class RelativeRange extends AstNode {
    public final boolean fromNow; // false means 'ago'
    public final Interval startInterval;
    public final Interval endInterval;

    RelativeRange(boolean fromNow, Ast.Interval startInterval,
        Ast.Interval endInterval) {
      super(Pos.ZERO, Op.RELATIVE);
      this.fromNow = fromNow;
      this.startInterval = startInterval;
      this.endInterval = endInterval;
    }

    @Override public Digester digest(Digester digester) {
      return super.digest(digester)
          .put("intervalType", intervalType())
          .sub("endInterval", endInterval)
          .sub("startInterval", startInterval);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      throw new AssertionError();
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
      throw new AssertionError();
    }

    public String intervalType() {
      return fromNow ? "from now" : "ago";
    }
  }

  /** E.g. "before 2 months from now", "before 3 days ago". */
  public static class RelativeUnit extends AstNode {
    public final boolean fromNow; // false means 'ago'
    public final BigDecimal value;
    public final DatetimeUnit unit;

    RelativeUnit(boolean before, boolean fromNow, BigDecimal value,
        DatetimeUnit unit) {
      super(Pos.ZERO, before ? Op.BEFORE : Op.AFTER);
      this.fromNow = fromNow;
      this.value = value;
      this.unit = unit;
    }

    @Override public Digester digest(Digester digester) {
      return super.digest(digester)
          .put("range", "relative")
          .put("fromnow", fromNow)
          .put("unit", unit.singular)
          .put("value", value);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      throw new AssertionError();
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
      throw new AssertionError();
    }
  }

  public static class Past extends AstNode {
    public final BigDecimal value;
    public final DatetimeUnit unit;
    public final boolean complete;

    Past(BigDecimal value, DatetimeUnit unit, boolean complete) {
      super(Pos.ZERO, Op.PAST);
      this.value = value;
      this.unit = unit;
      this.complete = complete;
    }

    @Override public Digester digest(Digester digester) {
      return super.digest(digester)
          .put("unit", unit.singular)
          .put("value", value)
          .putIf("complete", complete ? true : null);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      throw new AssertionError();
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
      throw new AssertionError();
    }
  }

  public static class Relative extends AstNode {
    public final BigDecimal value;
    public final DatetimeUnit unit;

    Relative(Op op, BigDecimal value, DatetimeUnit unit) {
      super(Pos.ZERO, op);
      checkArgument(op == Op.FROM_NOW || op == Op.PAST_AGO);
      this.value = value;
      this.unit = unit;
    }

    @Override public Digester digest(Digester digester) {
      return super.digest(digester)
          .put("unit", unit.singular)
          .put("value", value);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      throw new AssertionError();
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
      throw new AssertionError();
    }
  }

  /** This or next unit, e.g. "this day", "next month". */
  public static class ThisUnit extends AstNode {
    public final DatetimeUnit unit;

    ThisUnit(Op op, DatetimeUnit unit) {
      super(Pos.ZERO, op);
      checkArgument(op == Op.THIS || op == Op.BEFORE_THIS || op == Op.AFTER_THIS
          || op == Op.NEXT || op == Op.BEFORE_NEXT || op == Op.AFTER_NEXT
          || op == Op.LAST || op == Op.BEFORE_LAST || op == Op.AFTER_LAST);
      this.unit = unit;
    }

    @Override public Digester digest(Digester digester) {
      return super.digest(digester)
          .put("unit", unit.singular);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      throw new AssertionError();
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
      throw new AssertionError();
    }
  }

  /** This range, e.g. "this day to second". */
  public static class ThisRange extends AstNode {
    public final DatetimeUnit startInterval;
    public final DatetimeUnit endInterval;

    ThisRange(DatetimeUnit startInterval, DatetimeUnit endInterval) {
      super(Pos.ZERO, Op.THIS_RANGE);
      this.startInterval = startInterval;
      this.endInterval = endInterval;
    }

    @Override public Digester digest(Digester digester) {
      return super.digest(digester)
          .put("startInterval", startInterval.singular)
          .put("endInterval", endInterval.singular);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      throw new AssertionError();
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
      throw new AssertionError();
    }
  }

  /** This range, e.g. "this day to second". */
  public static class LastInterval extends AstNode {
    public final BigDecimal value;
    public final DatetimeUnit unit;

    LastInterval(BigDecimal value, DatetimeUnit unit) {
      super(Pos.ZERO, Op.LAST_INTERVAL);
      this.value = value;
      this.unit = unit;
    }

    @Override public Digester digest(Digester digester) {
      return super.digest(digester)
          .put("unit", unit.singular)
          .put("value", value);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      throw new AssertionError();
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
      throw new AssertionError();
    }
  }
}

// End Ast.java
