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

import static java.util.Objects.requireNonNull;

/** Error handler that converts LookML validation errors into strings,
 * and appends those strings to a given consumer. */
class LoggingErrorHandler implements ErrorHandler {
  private final Consumer<String> consumer;

  LoggingErrorHandler(Consumer<String> consumer) {
    this.consumer = requireNonNull(consumer);
  }

  @Override public void invalidRootProperty(String propertyName) {
    consumer.accept("invalidRootProperty(" + propertyName + ")");
  }

  @Override public void invalidPropertyOfParent(String propertyName,
      String parent) {
    consumer.accept("invalidPropertyOfParent(" + propertyName
        + ", " + parent + ")");
  }

  @Override public void nameRequired(String propertyName) {
    consumer.accept("nameRequired(" + propertyName + ")");
  }

  @Override public void invalidPropertyType(String propertyName,
      LookmlSchema.Type type, LookmlSchema.Type actualType) {
    consumer.accept("invalidPropertyType(" + propertyName
        + ", " + type + ", " + actualType + ")");
  }

  @Override public void invalidEnumValue(String parentTypeName,
      String propertyName, String typeName, String value) {
    consumer.accept("invalidPropertyType(" + parentTypeName
        + ", " + propertyName + ", " + typeName + ", " + value + ")");
  }

  @Override public void invalidListElement(String propertyName,
      LookmlSchema.Type actualElementType, LookmlSchema.Type listType) {
    consumer.accept("invalidListElement(" + propertyName + ", "
        + actualElementType + ", " + listType + ")");
  }
}
