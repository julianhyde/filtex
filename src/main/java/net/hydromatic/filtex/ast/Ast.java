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

import static java.util.Objects.requireNonNull;

/** Various subclasses of AST nodes. */
public class Ast {
  private Ast() {
  }

  /** Geographical box. */
  public static class Point extends AstNode {
    public final Location location;

    protected Point(Location location) {
      super(Pos.ZERO, Op.POINT);
      this.location = location;
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

  /** Range. */
  public static class Range extends AstNode {
    public final boolean is;
    public final BigDecimal left;
    public final BigDecimal right;

    public Range(Op op, String id, boolean is, BigDecimal left,
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
}

// End Ast.java
