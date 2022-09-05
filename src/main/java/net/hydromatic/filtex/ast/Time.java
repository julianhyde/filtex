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

/** Time value, e.g. "12:34:56".
 *
 * @see Date
 * @see Datetime */
public class Time {
  public final int hour;
  public final int minute;
  public final @Nullable Integer second;

  public Time(int hour, int minute, @Nullable Integer second) {
    this.hour = hour;
    this.minute = minute;
    this.second = second;
  }
}

// End Time.java
