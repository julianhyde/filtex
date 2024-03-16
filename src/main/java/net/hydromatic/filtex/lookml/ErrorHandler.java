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

/** Handles the various types of errors that can occur when validating
 * a LookML document against a {@link LookmlSchema}. */
public interface ErrorHandler {
  /** Called when you have used a property in the root of a LookML document
   * that is not valid. Root properties must be named-objects. Typically,
   * "model" is the only root property. */
  void invalidRootProperty(String propertyName);

  /** Called when you have used a property that is not valid within its
   * parent object. */
  void invalidPropertyOfParent(String propertyName, String parent);

  /** Called when you have used an object property that requires a name
   * but have not provided a name. For example, "dimension: {}". */
  void nameRequired(String propertyName);

  /** Called when you have given property {@code actualType} but its
   * value should have type {@code type}. */
  void invalidPropertyType(String propertyName, LookmlSchema.Type type,
      LookmlSchema.Type actualType);

  /** Called when the value of an enum property is not valid for the enum
   * type. */
  void invalidEnumValue(String parentTypeName, String propertyName,
      String typeName, String value);

  /** Called when an element in a list does not match the list type. */
  void invalidListElement(String propertyName,
      LookmlSchema.Type actualElementType, LookmlSchema.Type listType);
}

// End ErrorHandler.java
