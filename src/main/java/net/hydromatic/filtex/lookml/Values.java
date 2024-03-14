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

import net.hydromatic.filtex.util.ImmutablePairList;
import net.hydromatic.filtex.util.PairList;

import java.util.List;

import static java.util.Objects.requireNonNull;

/** Implementations of {@link Value}. */
class Values {
  private Values() {}

  static Value number(Number value) {
    return new NumberValue(value);
  }

  static Value identifier(String value) {
    return new IdentifierValue(value);
  }

  static Value bool(boolean value) {
    return new BooleanValue(value);
  }

  static Value string(String value) {
    return new StringValue(value);
  }

  static Value code(String value) {
    return new CodeValue(value);
  }

  static Value list(List<Value> list) {
    return new ListValue(list);
  }

  static Value namedObject(String name, PairList<String, Value> properties) {
    return new NamedObjectValue(name, properties);
  }

  static Value object(PairList<String, Value> properties) {
    return new ObjectValue(properties);
  }

  /** Value of a property or list element whose value is an identifier. */
  static class IdentifierValue extends Value {
    private final String id;

    IdentifierValue(String id) {
      this.id = id;
    }

    @Override void write(LookmlWriter writer) {
      writer.identifier(id);
    }
  }

  /** Value of a property or list element whose value is a number. */
  static class NumberValue extends Value {
    private final Number number;

    NumberValue(Number number) {
      this.number = number;
    }

    @Override void write(LookmlWriter writer) {
      writer.number(number);
    }
  }

  /** Value of a property or list element whose value is a boolean. */
  static class BooleanValue extends Value {
    private final boolean b;

    BooleanValue(boolean b) {
      this.b = b;
    }

    @Override void write(LookmlWriter writer) {
      writer.bool(b);
    }
  }

  /** Value of a property or list element whose value is a string. */
  static class StringValue extends Value {
    private final String s;

    StringValue(String s) {
      this.s = s;
    }

    @Override void write(LookmlWriter writer) {
      writer.string(s);
    }
  }

  /** Value of a property whose value is a code block. */
  static class CodeValue extends Value {
    private final String s;

    CodeValue(String s) {
      this.s = s;
    }

    @Override void write(LookmlWriter writer) {
      writer.code(s);
    }
  }

  /** Value of a property or list element whose value is a list. */
  static class ListValue extends Value {
    private final List<Value> list;

    ListValue(List<Value> list) {
      this.list = list;
    }

    @Override void write(LookmlWriter writer) {
      writer.list(list);
    }
  }

  /** Value of a property whose value is an object.
   *
   * <p>For example,
   * <blockquote><pre>{@code
   * conditionally_filter: {
   *   filters: [f3: "> 10"]
   *   unless: [f1, f2]
   * }
   * }</pre></blockquote>
   *
   * <p>The name of the property, {@code conditionally_filter}, will be held
   * in the enclosing property list. */
  static class ObjectValue extends Value {
    final ImmutablePairList<String, Value> properties;

    ObjectValue(PairList<String, Value> properties) {
      this.properties = ImmutablePairList.copyOf(properties);
    }

    @Override void write(LookmlWriter writer) {
      writer.obj(properties);
    }
  }

  /** Value of a property whose value is an object and that also has a name.
   *
   * <p>For example,
   * <blockquote><pre>{@code
   * dimension: d1 {
   *   sql: orderDate;;
   *   type: int
   * }
   * }</pre></blockquote>
   *
   * <p>{@link #name} is "d1", and {@link #properties} has entries for "sql" and
   * "type". The name of the property, {@code dimension}, is held in the
   * enclosing property list. */
  static class NamedObjectValue extends ObjectValue {
    final String name;

    NamedObjectValue(String name, PairList<String, Value> properties) {
      super(properties);
      this.name = requireNonNull(name);
    }

    @Override void write(LookmlWriter writer) {
      // Write "name { ... }"
      writer.identifier(name);
      writer.buf.append(' ');
      super.write(writer);
    }
  }
}

// End Values.java
