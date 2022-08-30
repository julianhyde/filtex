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

    @Override
    public AstWriter unparse(AstWriter writer) {
      return null;
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
    }

    @Override public Asts.Model model() {
      return null;
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
      return null;
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
    }

    @Override public Asts.Model model() {
      return null;
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
      return null;
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
    }

    @Override public Asts.Model model() {
      return null;
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

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
      visitor.visit(this, parent);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      String sep = "";
      for (Comparable comparable : value) {
        writer.append(sep);
        writer.appendLiteral(comparable);
        sep = ",";
      }
      return writer;
    }

    @Override public Asts.Model model() {
      return new Asts.Model(id, is, op.s, value, null, null, null);
    }
  }

  /** Call with zero arguments, optionally negated. */
  public static class Call0 extends AstNode {
    final boolean is;

    Call0(Op op, boolean is) {
      super(Pos.ZERO, op);
      this.is = is;
    }

    @Override public AstWriter unparse(AstWriter writer) {
      if (!is) {
        writer.append("not ");
      }
      return writer.append(op.s);
    }

    @Override public void accept(AstVisitor visitor, @Nullable AstNode parent) {
      visitor.visit(this, parent);
    }

    @Override public Asts.Model model() {
      return new Asts.Model(id, is, op.s, null, null, null, null);
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

    @Override public Asts.Model model() {
      return new Asts.Model(id, is, op.s, null, "", null, null);
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

    @Override public Asts.Model model() {
      return new Asts.Model(id, true, op.s,
          ImmutableList.of(left.toString(), right.toString()),
          null, null, null);
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

    @Override public Asts.Model model() {
      final Iterable<Comparable> value =
          left != null && right != null ? ImmutableList.of(left, right)
              : left != null ? ImmutableList.of(left)
                  : ImmutableList.of(right);
      return new Asts.Model(id, is, type(), value, op.s,
          left == null ? "" : left.toString(),
          right == null ? "" : right.toString());
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

    @Override public Asts.Model model() {
      return new Asts.Model(id, true, op.s, ImmutableList.of(expression), null,
          null, null);
    }
  }
}

// End Ast.java
