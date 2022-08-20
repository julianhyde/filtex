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
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/** Various sub-classes of AST nodes. */
public class Ast {
  private Ast() {
  }

  /** Literal. */
  @SuppressWarnings("rawtypes")
  public static class Comparison extends AstNode {
    final boolean is;
    public final Comparable value;

    Comparison(boolean is, Op op, Comparable value) {
      super(Pos.ZERO, op);
      this.is = is;
      this.value = value;
    }

    @Override public void accept(Visitor visitor, @Nullable AstNode parent) {
      visitor.visit(this, parent);
    }

    @Override public AstWriter unparse(AstWriter writer) {
      return writer.appendLiteral(value);
    }

    @Override public Asts.Model model() {
      return new Asts.Model(is, op.s, value, null, null, null);
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

    @Override public void accept(Visitor visitor, @Nullable AstNode parent) {
      visitor.visit(this, parent);
    }

    @Override public Asts.Model model() {
      return new Asts.Model(is, op.s, null, null, null, null);
    }
  }

  /** Call with one argument, optionally negated. */
  public static class Call1 extends AstNode {
    final boolean is;
    final AstNode node;

    Call1(Op op, boolean is, AstNode node) {
      super(Pos.ZERO, op);
      this.is = is;
      this.node = node;
    }

    @Override public void accept(Visitor visitor, @Nullable AstNode parent) {
      visitor.visit(this, parent);
    }

    @Override public Asts.Model model() {
      return new Asts.Model(is, op.s, null, "", null, null);
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
    public final AstNode right;

    Call2(Op op, AstNode left, AstNode right) {
      super(Pos.ZERO, op);
      this.left = left;
      this.right = right;
    }

    @Override public AstWriter unparse(AstWriter writer) {
      return writer.infix(left, op, right);
    }

    @Override public void accept(Visitor visitor, AstNode parent) {
      visitor.visit(this, parent);
    }

    @Override public Asts.Model model() {
      return new Asts.Model(true, op.s,
          ImmutableList.of(left.toString(), right.toString()),
          null, null, null);
    }
  }

  /** Range. */
  public static class Range extends AstNode {
    public final boolean is;
    public final BigDecimal left;
    public final BigDecimal right;

    public Range(Op op, boolean is, BigDecimal left, BigDecimal right) {
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

    @Override public void accept(Visitor visitor, AstNode parent) {
      visitor.visit(this, parent);
    }

    @Override public Asts.Model model() {
      final String type;
      switch (op) {
      case ABSENT_CLOSED:
      case ABSENT_OPEN:
      case CLOSED_ABSENT:
      case OPEN_ABSENT:
        type = op.s;
        break;
      default:
        type = "between";
      }
      final Object value =
          left != null && right != null ? ImmutableList.of(left, right)
              : left != null ? left
                  : right;
      return new Asts.Model(is, type, value, op.s,
          left == null ? "" : left.toString(),
          right == null ? "" : right.toString());
    }
  }
}

// End Ast.java
