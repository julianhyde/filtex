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

import com.google.common.base.CaseFormat;

import org.checkerframework.checker.nullness.qual.Nullable;

/** Parse tree operator. */
public enum Op {
  EQ("="),
  GE(">="),
  GT(">"),
  LE("<="),
  LT("<"),
  COMMA(","),
  LITERAL(""),
  NULL,
  NOTNULL,
  OPEN_OPEN("()", "(", ")"),
  OPEN_CLOSED("(]", "(", "]"),
  OPEN_ABSENT(">", "(", "inf)"),
  CLOSED_OPEN("[)", "[", ")"),
  CLOSED_CLOSED("[]", "[", "]"),
  CLOSED_ABSENT(">=", "[", "inf)"),
  ABSENT_OPEN("<", "(-inf", ")"),
  ABSENT_CLOSED("<=", "(-inf", "]"),
  MATCHES_ADVANCED,

  // location

  ANYWHERE("is anywhere"),
  BOX,
  CIRCLE,
  POINT("location"),

  // date

  YEAR,
  FISCAL_YEAR,
  QUARTER,
  FISCAL_QUARTER,
  MONTH,
  ON,
  RANGE_INTERVAL,
  RANGE,
  MONTH_INTERVAL,
  INTERVAL,
  BEFORE,
  AFTER,
  AFTER_THIS("after_this"),
  BEFORE_THIS("before_this"),
  AFTER_NEXT("after_next"),
  BEFORE_NEXT("before_next"),
  AFTER_LAST("after_last"),
  BEFORE_LAST("before_last"),
  RELATIVE,
  THIS,
  NEXT,
  LAST,
  THIS_RANGE,
  PAST,
  PAST_AGO,
  FROM_NOW("from now"),
  DAY,
  LAST_INTERVAL;

  public final String s;
  public final String left;
  public final String right;

  Op(String s, String left, String right) {
    this.s = s;
    this.left = left;
    this.right = right;
  }

  Op(@Nullable String s) {
    this.s = s != null ? s
        : CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name());
    this.left = null;
    this.right = null;
  }

  Op() {
    this(null);
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

  public Op beforeAfter(boolean before) {
    switch (this) {
    case THIS:
      return before ? BEFORE_THIS : AFTER_THIS;
    case NEXT:
      return before ? BEFORE_NEXT : AFTER_NEXT;
    case LAST:
      return before ? BEFORE_LAST : AFTER_LAST;
    default:
      throw new IllegalArgumentException((before ? "before" : "after")
          + "-" + this);
    }
  }
}

// End Op.java
