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
import net.hydromatic.filtex.ast.AstWriter;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/** Matchers for use in Filtex tests. */
public abstract class Matchers {
  private Matchers() {}

  /** Matches a literal by value. */
  @SuppressWarnings("rawtypes")
  static Matcher<Ast.Comparison> isComparison(Comparable comparable, String s) {
    return new TypeSafeMatcher<Ast.Comparison>() {
      protected boolean matchesSafely(Ast.Comparison literal) {
        final String actual = literal.toString();
        return literal.value.size() == 1
            && literal.value.get(0).equals(comparable)
            && actual.equals(s);
      }

      public void describeTo(Description description) {
        description.appendText("literal with value " + comparable
            + " and source " + s);
      }
    };
  }

  /** Matches an AST node by its string representation. */
  static Matcher<AstNode> isAst(String expected) {
    return isAst(AstNode.class, true, expected);
  }

  /** Matches an AST node by its string representation. */
  static <T extends AstNode> Matcher<T> isAst(Class<? extends T> clazz,
      boolean parenthesize, String expected) {
    return new CustomTypeSafeMatcher<T>("ast with value [" + expected + "]") {
      protected boolean matchesSafely(T t) {
        assertThat(clazz.isInstance(t), is(true));
        final String s =
            stringValue(t);
        return s.equals(expected);
      }

      private String stringValue(T t) {
        return t.unparse(new AstWriter()).toString();
      }

      @Override protected void describeMismatchSafely(T item,
          Description description) {
        description.appendText("was ").appendValue(stringValue(item));
      }
    };
  }


}

// End Matchers.java
