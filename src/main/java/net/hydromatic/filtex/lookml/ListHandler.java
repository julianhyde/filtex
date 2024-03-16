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
package net.hydromatic.filtex.lookml;

import java.util.function.Consumer;

/** Handles events that occur while parsing a LookML list.
 *
 * @see ObjectHandler */
public interface ListHandler {
  /** Adds a string element to this list. */
  ListHandler string(String value);

  /** Adds a numeric element to this list. */
  ListHandler number(Number value);

  /** Adds a boolean element to this list. */
  ListHandler bool(boolean value);

  /** Adds an identifier to this list. */
  ListHandler identifier(String value);

  /** Adds a string-identifier pair to this list. */
  ListHandler pair(String ref, String identifier);

  /** Adds a comment to this list. */
  ListHandler comment(String comment);

  /** Finishes this list. */
  void close();

  /** Adds an element to this list whose value is a list,
   * and returns the handler for the sub-list.
   *
   * <p>Unlike {@link #list}, thos method does not close the sub-list. The
   * caller must remember to call {@link #close}.
   */
  ListHandler listOpen();

  default ListHandler list(Consumer<ListHandler> consumer) {
    ListHandler h = listOpen();
    consumer.accept(h);
    h.close();
    return this;
  }
}
