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

/** Date value, e.g. "2018/09/05".
 *
 * <p>The subclass {@link Datetime} also includes a Time value. */
public class Interval {
  public final int year;
  public final int month;
  public final int date;

  public Interval(int year, int month, int date) {
    this.year = year;
    this.month = month;
    this.date = date;
  }

  public Datetime plus(Time t) {
    return new Datetime(year, month, date, t.hour, t.minute, t.second);
  }
}

// End Date.java
