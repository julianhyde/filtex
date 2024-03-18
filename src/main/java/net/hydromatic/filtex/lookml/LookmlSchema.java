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

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

/** Defines what properties are valid in a LookML document.
 *
 * <p>Instances of {@code LookmlSchema} are immutable and thread-safe.
 * Typically, a schema is created on bootstrap by reading the schema file
 * for the current version of LookML, and that same instance is used whenever
 * a validating parser is required. */
public interface LookmlSchema {
  /** Returns the name of this schema. */
  String name();

  /** Returns properties that may occur at the root of a model.
   *
   * <p>In the standard schema, this is just "[model]".
   */
  Map<String, Property> rootProperties();

  /** Returns all object types.
   *
   * <p>In the standard schema, includes "model", "view", "dimension",
   * "join". */
  Map<String, ObjectType> objectTypes();

  /** Returns all enum types.
   *
   * <p>In the standard schema, includes "join_type" (which is used by the
   * "type" property of the "join" object), "boolean", "timeframe".
   */
  Map<String, EnumType> enumTypes();

  /** Returns the names of properties that have code values.
   * If a property sometimes has a code value and sometimes another
   * type, it is an error. */
  SortedSet<String> codePropertyNames();

  /** Describes the value-type of a property. */
  enum Type {
    /** Numeric value. E.g. '{@code precision: 10}'. */
    NUMBER,

    /** String value. E.g. '{@code value_format: "$#.00;($#.00)"}'. */
    STRING,

    /** Enumerated value. E.g. '{@code type: left_outer}',
     * '{@code primary_key: yes}'. */
    ENUM,

    /** Code block value.
     * E.g. '{@code sql_on: orders.customer_id = customer.id ;;}'. */
    CODE,

    /** Object value.
     *
     * <p>E.g. {@code conditional_filter},
     * <blockquote><pre>{@code
     *   conditional_filter: {
     *     filters: []
     *     unless: []
     *   }
     * }</pre></blockquote>
     */
    OBJECT,

    /** Named-object value.
     *
     * <p>E.g. {@code dimension},
     * <blockquote><pre>{@code
     *   dimension: order_date {
     *     sql: orders.order_date;;
     *   }
     * }</pre></blockquote>
     */
    NAMED_OBJECT,

    /** Value that is a reference to an object in this model,
     * e.g. '{@code from: orders}'. */
    REF,

    /** Value that is a list of references to an objects in this model,
     * e.g. '{@code drill_fields: [id, name, city, state]}'. */
    REF_LIST,

    /** Value that is a list of strings,
     * e.g. '{@code tags: ["abc", "wyxz"]}'. */
    STRING_LIST,

    /** Value that is a list of reference-string pairs,
     * e.g. '{@code filters: [id: "123", customer.id: "789"]}'. */
    REF_STRING_MAP,

    /** Value that is a reference-string pair,
     * e.g. '{@code id: "123"}'.
     *
     * <p>Never occurs as a property, only as an element of a list of type
     * {@link #REF_STRING_MAP}.*/
    REF_STRING
  }

  /** Describes a LookML type that has a fixed set of values.
   *
   * <p>Examples include the boolean type (values true and false),
   * join type (values left_outer, inner). */
  interface EnumType {
    /** Returns the allowed values of this enum type.
     *
     * <p>The set is sorted alphabetically, and does not contain nulls. */
    SortedSet<String> allowedValues();
  }

  /** Describes a LookML type that may have nested properties.
   *
   * <p>Examples include "model", "dimension", "always_filter".
   */
  interface ObjectType {
    /** Returns a collection of properties that an object of this type may
     * have. */
    SortedMap<String, Property> properties();
  }

  /** The schema of a LookML property. */
  interface Property {
    /** Returns the name of the property. */
    String name();

    /** Returns the type of the property's values. */
    Type type();

    /** Name with which to look up an
     * {@link EnumType} in {@link LookmlSchema#enumTypes()}
     * or an {@link ObjectType} in {@link LookmlSchema#objectTypes}.
     *
     * <p>Returns empty string if {@link #type()} is not
     * {@link Type#OBJECT},
     * {@link Type#NAMED_OBJECT}, or
     * {@link Type#ENUM}. */
    String typeName();
  }
}

// End LookmlSchema.java
