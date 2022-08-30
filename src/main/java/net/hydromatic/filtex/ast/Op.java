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

/** Parse tree operator. */
public enum Op {
  EQ("="),
  GE(">="),
  GT(">"),
  LE("<="),
  LT("<"),
  COMMA(","),
  LITERAL(""),
  NULL("null"),
  OPEN_OPEN("()", "(", ")"),
  OPEN_CLOSED("(]", "(", "]"),
  OPEN_ABSENT(">", "(", "inf)"),
  CLOSED_OPEN("[)", "[", ")"),
  CLOSED_CLOSED("[]", "[", "]"),
  CLOSED_ABSENT(">=", "[", "inf)"),
  ABSENT_OPEN("<", "(-inf", ")"),
  ABSENT_CLOSED("<=", "(-inf", "]"),
  MATCHES_ADVANCED("matchesAdvanced"),

  // location

  ANYWHERE("anywhere"),
  BOX("box"),
  CIRCLE("circle"),
  POINT("location");

  public final String s;
  public final String left;
  public final String right;

  Op(String s, String left, String right) {
    this.s = s;
    this.left = left;
    this.right = right;
  }

  Op(String s) {
    this.s = s;
    this.left = null;
    this.right = null;
  }

  /** Returns whether this operation is a range that is closed below;
   * for example "[0, 10)" contains its lower bound, "0". */
  public boolean containsLowerBound() {
    return s.startsWith("[");
  }

  /** Returns whether this operation is a range that is closed above;
   * for example "[0, 10)" does not contain its lower bound, "10". */
  public boolean containsUpperBound() {
    return s.endsWith("]");
  }
}

// End Op.java
