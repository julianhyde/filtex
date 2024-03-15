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

/** Handles events that occur while parsing a LookML object.
 *
 * @see ListHandler */
public interface ObjectHandler {
  /** Adds a numeric property. */
  ObjectHandler number(String propertyName, Number value);

  /** Adds a boolean property. */
  ObjectHandler bool(String propertyName, boolean value);

  /** Adds a string property. */
  ObjectHandler string(String propertyName, String value);

  /** Adds an identifier (unquoted string) property. */
  ObjectHandler identifier(String propertyName, String value);

  /** Adds a code  property. (E.g. "sql: select * from orders;;". */
  ObjectHandler code(String propertyName, String value);

  /** Starts a property whose value is a list. */
  ListHandler listOpen(String propertyName);

  /** Starts and ends a property whose value is a list. */
  default ObjectHandler list(String propertyName,
      Consumer<ListHandler> consumer) {
    ListHandler h = listOpen(propertyName);
    consumer.accept(h);
    h.close();
    return this;
  }

  /** Starts a property whose value is an object, and returns the handler
   * for the sub-object.
   *
   * <p>Unlike {@link #obj(String, Consumer)},
   * this method does not close the object: the caller
   * must remember to call {@link #close}. */
  ObjectHandler objOpen(String property);

  /** Starts and ends a property whose value is an object, calling a given
   * consumer to allow the user to provide the contents of the object. */
  default ObjectHandler obj(String propertyName,
      Consumer<ObjectHandler> consumer) {
    final ObjectHandler h = objOpen(propertyName);
    consumer.accept(h);
    h.close();
    return this;
  }

  /** Starts a property whose value is a named object, and returns the handler
   * for the sub-object.
   *
   * <p>Unlike {@link #obj(String, String, Consumer)},
   * this method does not close the object: the caller
   * must remember to call {@link #close}. */
  ObjectHandler objOpen(String propertyName, String name);

  /** Starts and ends a property whose value is a named object, calling a given
   * consumer to allow the user to provide the contents of the object. */
  default ObjectHandler obj(String propertyName, String name,
      Consumer<ObjectHandler> consumer) {
    final ObjectHandler h = objOpen(propertyName, name);
    consumer.accept(h);
    h.close();
    return this;
  }

  /** Closes this object. */
  void close();

  ObjectHandler comment(String comment);
}
