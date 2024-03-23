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

/** Handler that converts LookML properties into strings, and appends those
 * strings to a given consumer. */
class LoggingPropertyHandler implements PropertyHandler {
  private final Consumer<String> consumer;
  private final ListHandler listHandler;

  LoggingPropertyHandler(Consumer<String> consumer) {
    this.consumer = consumer;
    this.listHandler = new LoggingHandler.LoggingListHandler(this.consumer);
  }

  @Override public PropertyHandler property(LookmlSchema.Property property,
      Object value) {
    consumer.accept("property(" + property.name()
        + ", " + property.type()
        + ", " + value + ")");
    return this;
  }

  @Override public ListHandler listOpen(LookmlSchema.Property property) {
    consumer.accept("listOpen(" + property.name()
        + ", " + property.type() + ")");
    return listHandler;
  }

  @Override public PropertyHandler objOpen(LookmlSchema.Property property) {
    consumer.accept("objOpen(" + property.name()
        + ", " + property.type() + ")");
    return this;
  }

  @Override public PropertyHandler objOpen(LookmlSchema.Property property,
      String name) {
    consumer.accept("objOpen(" + property.name()
        + ", " + property.type()
        + ", " + name + ")");
    return this;
  }

  @Override public void close() {
    consumer.accept("objClose()");
  }
}
