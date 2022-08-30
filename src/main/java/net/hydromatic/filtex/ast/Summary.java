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

import net.hydromatic.filtex.Filtex;
import net.hydromatic.filtex.TypeFamily;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

/** Summary. */
public class Summary {
  private Summary() {}

  /** Builds a summary description for a filter expression. */
  public static String summary(TypeFamily typeFamily,
      @Nullable String expression,
      List<Object> userAttributes,
      @Nullable String field,
      boolean required) {
    if (required && expression == null) {
      throw new IllegalArgumentException("value required");
    }
    AstNode node = Filtex.parseFilterExpression(typeFamily, expression);
    return summary(typeFamily, node, expression, userAttributes);
  }

  /** Builds a summary description for a filter expression. */
  public static String summary(TypeFamily typeFamily, AstNode node,
      @Nullable String expression, List<Object> userAttributes) {
    // Special case: user attribute should be displayed
    // as the name and a "(null)" text if it has no value
    if (hasUserAttributeNodeWithoutValue(node)) {
      final Object userAttribute =
          getUserAttributeMatchingAst(node, userAttributes);
      if (userAttribute != null) {
        return ""; // `${userAttribute!.label} (null)`
      }
    }
    return typeFamily.describe(node);
  }

  private static String locationSummary(AstNode node) {
    return null;
  }

  private static @Nullable Object getUserAttributeMatchingAst(AstNode node,
      List<Object> userAttributes) {
    return null;
  }

  private static boolean hasUserAttributeNodeWithoutValue(AstNode node) {
    return false;
  }

  public static String describeLocation(AstNode node) {
    return null;
  }
}

// End Summary.java
