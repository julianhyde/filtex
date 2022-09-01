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
import java.util.function.BinaryOperator;

/** AST Builder. */
public enum AstBuilder {
  /** The singleton instance of the AST builder.
   * The short name is convenient for use via 'import static',
   * but checkstyle does not approve. */
  // CHECKSTYLE: IGNORE 1
  ast;

  /** Creates an anywhere location. */
  public AstNode anywhere() {
    return new Ast.Call0(Op.ANYWHERE, true);
  }

  /** Creates a point geographical region. */
  public AstNode point(Location location) {
    return new Ast.Point(location);
  }

  /** Creates a box-shaped geographical region. */
  public AstNode box(Location from, Location to) {
    return new Ast.Box(from, to);
  }

  /** Creates a circular geographical region. */
  public AstNode circle(BigDecimal distance, Unit unit, Location location) {
    return new Ast.Circle(distance, unit, location);
  }

  /** Creates a comparison. */
  @SuppressWarnings("rawtypes")
  public Ast.Comparison comparison(boolean is, Op op, Comparable value) {
    return comparison(is, op, ImmutableList.of(value));
  }

  @SuppressWarnings("rawtypes")
  public Ast.Comparison comparison(boolean is, Op op,
      Iterable<Comparable> value) {
    return new Ast.Comparison(is, op, ImmutableList.copyOf(value));
  }

  /** Creates a number literal. */
  public Ast.Comparison numberLiteral(boolean is, BigDecimal value) {
    return comparison(is, Op.EQ, ImmutableList.of(value));
  }

  /** Creates a string literal. */
  public Ast.Comparison stringLiteral(boolean is, String value) {
    return comparison(is, Op.EQ, ImmutableList.of(value));
  }

  /** Creates a logical expression. */
  public AstNode logicalExpression(AstNode left, AstNode right) {
    return new Ast.Call2(Op.COMMA, left, right);
  }

  /** Folds a list into a left-deep a logical expression. */
  public AstNode logicalExpression(List<AstNode> terms) {
    return foldRight(terms, this::logicalExpression);
  }

  /** Creates a term representing "null" or "not null". */
  public AstNode isNull(boolean is) {
    return new Ast.Call0(Op.NULL, is);
  }

  /** Creates a term representing "not null" (for location). */
  public AstNode isNotNull() {
    return new Ast.Call0(Op.NOTNULL, true);
  }

  /** Creates a term representing a range, such as "{@code [0, 10)}", or a
   * comparison, such as "{@code > 5}". */
  public AstNode between(boolean is, Bound leftBound, Bound rightBound,
      @Nullable BigDecimal left, @Nullable BigDecimal right) {
    if (left != null && right != null) {
      return range(getOp(leftBound, rightBound), is, left, right);
    } else if (left != null) {
      return comparison(is, leftBound == Bound.CLOSED ? Op.GE : Op.GT, left);
    } else if (right != null) {
      return comparison(is, rightBound == Bound.CLOSED ? Op.LE : Op.LT, right);
    } else {
      throw new IllegalArgumentException();
    }
  }

  private Ast.Range range(Op op, boolean is, BigDecimal left,
      BigDecimal right) {
    return new Ast.Range(op, null, is, left, right);
  }

  /** Creates a term representing a one-sided range, such as "{@code > 10}"
   * or "{@code <= 0}". */
  public AstNode between(Op op, boolean is, BigDecimal number) {
    switch (op) {
    case ABSENT_OPEN:
    case ABSENT_CLOSED:
      return comparison(is, op == Op.ABSENT_OPEN ? Op.LT : Op.LE, number);
    case OPEN_ABSENT:
    case CLOSED_ABSENT:
      return comparison(is, op == Op.OPEN_ABSENT ? Op.GT : Op.GE, number);
    default:
      throw new AssertionError("unknown " + op);
    }
  }

  /** Creates a matchesAdvanced. */
  @SuppressWarnings("rawtypes")
  public Ast.MatchesAdvanced matchesAdvanced(String expression) {
    return new Ast.MatchesAdvanced(expression);
  }

  /** Converts a pair of bounds into an operator.
   * For example {@code getOp(ABSENT, OPEN)} returns {@link Op#ABSENT_OPEN}. */
  private Op getOp(Bound left, Bound right) {
    return Op.valueOf(left + "_" + right);
  }

  /** Folds a list to the left.
   * Thus [1, 2, 3, 4] becomes (((1, 2), 3), 4). */
  private static <E> E foldLeft(Iterable<? extends E> iterable,
      BinaryOperator<E> combiner) {
    E previous = null;
    for (E e : iterable) {
      if (previous == null) {
        previous = e;
      } else {
        previous = combiner.apply(previous, e);
      }
    }
    if (previous == null) {
      throw new IllegalArgumentException("empty list");
    }
    return previous;
  }

  /** Folds a list to the right.
   * Thus [1, 2, 3, 4] becomes (1, (2, (3, 4))). */
  private static <E> E foldRight(Iterable<? extends E> iterable,
      BinaryOperator<E> combiner) {
    return foldLeft(ImmutableList.copyOf(iterable).reverse(),
        (BinaryOperator<E>) (x, y) -> combiner.apply(y, x));
  }
}

// End AstBuilder.java
