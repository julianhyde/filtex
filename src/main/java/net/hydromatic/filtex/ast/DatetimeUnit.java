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

import net.hydromatic.filtex.util.Pair;

import com.google.common.base.CaseFormat;

import java.math.BigDecimal;
import java.util.Locale;

/** Unit of time. */
public enum DatetimeUnit {
  YEAR,
  FISCAL_YEAR,
  MONTH,
  WEEK,
  QUARTER,
  FISCAL_QUARTER,
  DAY,
  HOUR,
  MINUTE,
  SECOND;

  public final String singular; // e.g. "hour"
  public final String plural; // e.g. "hours"

  DatetimeUnit() {
    this.singular =
        CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name());
    this.plural = singular + "s";
  }

  /** Looks up a unit. Can handle upper and lower case, singular and plural,
   * e.g. "YEAR", "year", "years". */
  public static DatetimeUnit of(String name) {
    name = name.toUpperCase(Locale.ROOT);
    if (name.endsWith("S")) {
      name = name.substring(0, name.length() - 1);
    }
    return DatetimeUnit.valueOf(name);
  }

  public Pair<BigDecimal, DatetimeUnit> times(BigDecimal multiplier) {
    return Pair.of(multiplier, this);
  }
}

// End DatetimeUnit.java
