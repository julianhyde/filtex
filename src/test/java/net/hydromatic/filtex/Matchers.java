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

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/** Matchers for use in Filtex tests. */
public abstract class Matchers {
  private Matchers() {}

  /** Matches a literal by value. */
  @SuppressWarnings("rawtypes")
  static Matcher<Ast.Literal> isLiteral(Comparable comparable, String s) {
    return new TypeSafeMatcher<>() {
      protected boolean matchesSafely(Ast.Literal literal) {
        final String actual = literal.toString();
        return literal.value.equals(comparable)
            && actual.equals(s);
      }

      public void describeTo(Description description) {
        description.appendText("literal with value " + comparable
            + " and source " + s);
      }
    };
  }
}

// End Matchers.java
