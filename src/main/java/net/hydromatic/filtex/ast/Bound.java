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

/** Type of bound of a range. */
public enum Bound {
  /** An open bound does not include its end-point. */
  OPEN,
  /** A closed bound includes its end-point. */
  CLOSED,
  /** An absent bound means that the range extends to infinity. */
  ABSENT;

  /** Returns the bound with the reverse sense.
   *
   * <p>An open bound, 'x > 0', becomes a closed bound when negated to 'x <= 0',
   * amd vice versa. */
  public Bound flip() {
    switch (this) {
    case OPEN:
      return CLOSED;
    case CLOSED:
      return OPEN;
    default:
      return this;
    }
  }
}

// End Bound.java
