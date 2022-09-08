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

import org.checkerframework.checker.nullness.qual.Nullable;

/** Date and time value, e.g. "2018/09/05 12:34:56".
 *
 * @see Time */
public class Datetime extends Date {
  public final int hour;
  public final int minute;
  public final @Nullable Integer second;

  public Datetime(int year, int month, int date, int hour, int minute,
      @Nullable Integer second) {
    super(year, month, date);
    this.hour = hour;
    this.minute = minute;
    this.second = second;
  }

  @Override public Digester digest(Digester digester) {
    return super.digest(digester)
        .put("hour", hour)
        .put("minute", minute)
        .putIf("second", second);
  }
}

// End Datetime.java
