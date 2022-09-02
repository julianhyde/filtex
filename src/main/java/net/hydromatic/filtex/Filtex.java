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
import net.hydromatic.filtex.ast.Summary;
import net.hydromatic.filtex.parse.FiltexParserImpl;
import net.hydromatic.filtex.parse.ParseException;
import net.hydromatic.filtex.parse.TokenMgrError;

import com.google.common.collect.ImmutableList;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.StringReader;
import java.util.Locale;

import static net.hydromatic.filtex.ast.AstBuilder.ast;

/**
 * Filter expressions.
 *
 * <p>Based on the
 * <a href="https://github.com/looker-open-source/components/tree/main/packages/filter-expressions">
 * &#064;looker/filter-expressions</a> TypeScript API.
 */
public class Filtex {
  private Filtex() {
  }

  /** Returns a valid filter expression type when given the type and field
   * properties of a dashboard filter.
   *
   * <p>For example,
   * <pre>{@code
   * getExpressionType(true, "field_filter")
   * }</pre>
   *
   * <p>returns {@link TypeFamily#NUMBER}.
   */
  public static TypeFamily getExpressionType(boolean numeric, String type) {
    if (numeric && type.equals("field_filter")) {
      return TypeFamily.NUMBER;
    }
    throw new UnsupportedOperationException();
  }

  /** Returns an Abstract Syntax Tree (AST) that logically represents the filter
   * expression string passed in, as well as the filter expression type (and
   * optional user attributes).
   *
   * <p>For example,
   * <pre>{@code
   * parseFilterExpression(TypeFamily.NUMBER, "[0,20],>30")
   * }</pre>
   *
   * <p>returns
   * <pre>{@code
   * {
   *    type: ',',
   *    left: {
   *      type: 'between',
   *      bounds: '[]',
   *      low: 0,
   *      high: 20,
   *      is: true,
   *    },
   *    right: {
   *      is: true,
   *      type: '>',
   *      value: [30],
   *    },
   * }
   * }</pre>
   */
  public static AstNode parseFilterExpression(TypeFamily typeFamily,
      String expression) {
    FiltexParserImpl parser =
        new FiltexParserImpl(new StringReader(expression));
    try {
      final AstNode node;
      switch (typeFamily) {
      case DATE:
        node = parser.dateExpressionEof();
        return Transforms.locationTransform(node);

      case LOCATION:
        node = parser.locationExpressionEof();
        return Transforms.locationTransform(node);

      case NUMBER:
        node = parser.numericExpressionEof();
        return Transforms.numberTransform(node);

      default:
        throw new IllegalArgumentException("unknown type family " + typeFamily);
      }
    } catch (ParseException | TokenMgrError e) {
      return getMatchesAdvancedNode(expression, null);
    }
  }

  private static AstNode getMatchesAdvancedNode(String expression,
      @Nullable AstNode node) {
    final Integer id;
    if (node != null) {
      if (node.expression() != null) {
        expression = node.expression();
      }
      id = node.id;
    } else {
      id = 1;
    }
    final Ast.MatchesAdvanced matchesAdvanced = ast.matchesAdvanced(expression);
    matchesAdvanced.id = id;
    return matchesAdvanced;
  }

  /** Returns a localized, human-readable summary of a
   * filter expression, given the expression's type, the expression itself,
   * and the user attributes and field, if applicable.
   *
   * <p>For example,
   * <pre>{@code
   * summary(TypeFamily.NUMBER, "[0,20],>30", Locale.EN_US)
   * }</pre>
   *
   * <p>returns {@code "is in range [0, 20] or is > 30"}. */
  public static String summary(TypeFamily typeFamily, String s,
      Locale locale) {
    return Summary.summary(typeFamily, s, ImmutableList.of(), null, true);
  }

  /**
   * Returns an object with utility functions and values pertaining to a given
   * expression type:
   *
   * <ul>
   *   <li>toString: a function that converts an AST into an expression of the
   *     given type
   *   <li>subTypes: an array containing the sub-types of the expression type,
   *     for example "{@code >}", "{@code <}", "{@code =}", "{@code between}"
   *     for a number expression type
   * </ul>
   */
  public static void typeToGrammar() {
    throw new UnsupportedOperationException();
  }

  /** Converts an AST to a single item for use in a token (i.e. not advanced)
   * filter. */
  public static void getFilterTokenItem() {
    throw new UnsupportedOperationException();
  }
}

// End Filtex.java
