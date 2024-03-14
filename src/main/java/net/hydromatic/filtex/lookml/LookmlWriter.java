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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/** Receives a stream of events and converts them into an indented LookML
 * string. */
public class LookmlWriter {
  final Sink sink;

  /** Hidden builder for the root element. Ordinary builders that have
   * a parent simply write to that parent, but this builder writes to
   * {@link #sink}. */
  final ObjectHandler rootHandler;

  /** Creates a writer.
   *
   * @param buf String builder to which to write the LookML
   * @param offset Number of spaces to increase indentation each time we enter
   *              a nested object or list
   * @param pretty Whether to pretty-print (with newlines and indentation) */
  public static LookmlWriter create(StringBuilder buf, boolean pretty,
      int offset) {
    return new LookmlWriter(new Sink(pretty, buf, offset));
  }

  private LookmlWriter(Sink sink) {
    this.sink = sink;
    this.rootHandler = new ObjectWriter(sink::propertyList);
  }

  /** Creates a consumer for the events that can occur within an object. */
  public ObjectHandler documentHandler() {
    return rootHandler;
  }

  /** The piece that does the actual work.
   * Contains a {@link java.lang.StringBuilder},
   * and keeps track of the current indentation level. */
  private static class Sink {
    /** Regex pattern for an identifier that does not need to be quoted. */
    static final Pattern SIMPLE_IDENTIFIER_PATTERN =
        Pattern.compile("[a-zA-Z0-9_]*");

    private final boolean pretty;
    private int indent = 0;
    private final int offset;
    final StringBuilder buf;

    Sink(boolean pretty, StringBuilder buf, int offset) {
      this.pretty = pretty;
      this.buf = buf;
      this.offset = offset;
    }

    /** Adds a string value. */
    void string(String s) {
      buf.append('"').append(s.replace("\"", "\\s")).append('"');
    }

    /** Adds a boolean value. */
    void bool(boolean b) {
      buf.append(b ? "yes" : "no");
    }

    /** Adds a numeric value. */
    void number(Number number) {
      buf.append(number);
    }

    /** Adds a code value. */
    void code(String s) {
      buf.append(s).append(";;");
    }

    /** Adds an object. */
    void obj(PairList<String, Value> properties) {
      if (properties.isEmpty()) {
        buf.append("{}");
      } else {
        buf.append('{');
        if (pretty) {
          buf.append('\n');
          indent += offset;
          Spaces.append(buf, indent);
        }
        propertyList(properties);
        if (pretty) {
          buf.append('\n');
          indent -= offset;
          Spaces.append(buf, indent);
        }
        buf.append('}');
      }
    }

    /** Adds a property list (an object without braces). */
    void propertyList(PairList<String, Value> properties) {
      properties.forEachIndexed((i, property, value) -> {
        if (i > 0) {
          if (pretty) {
            buf.append('\n');
            Spaces.append(buf, indent);
          } else {
            buf.append(", ");
          }
        }
        value.write(property, this);
      });
    }

    void label(String name) {
      identifier(name);
      buf.append(pretty ? ": " : ":");
    }

    void identifier(String id) {
      Matcher m = SIMPLE_IDENTIFIER_PATTERN.matcher(id);
      if (m.matches()) {
        buf.append(id);
      } else {
        string(id);
      }
    }

    /** Adds a list value. */
    void list(List<Value> list) {
      if (list.isEmpty()) {
        buf.append("[]");
        return;
      }
      buf.append('[');
      indent += offset;
      for (int i = 0; i < list.size(); i++) {
        Value value = list.get(i);
        if (i > 0) {
          if (pretty) {
            buf.append(",\n");
            Spaces.append(buf, indent);
          } else {
            buf.append(", ");
          }
        } else {
          if (pretty) {
            buf.append('\n');
            Spaces.append(buf, indent);
          }
        }
        value.write(this);
      }
      indent -= offset;
      if (pretty) {
        buf.append('\n');
        Spaces.append(buf, indent);
      }
      buf.append(']');
    }
  }

  /** Wrapper around a value in a property or a list. */
  abstract static class Value {
    abstract void write(Sink sink);

    void write(String property, Sink sink) {
      sink.label(property);
      write(sink);
    }
  }

  /** Value of a property or list element whose value is an identifier. */
  static class IdentifierValue extends Value {
    private final String id;

    IdentifierValue(String id) {
      this.id = id;
    }

    @Override void write(Sink sink) {
      sink.identifier(id);
    }
  }

  /** Value of a property or list element whose value is a number. */
  static class NumberValue extends Value {
    private final Number number;

    NumberValue(Number number) {
      this.number = number;
    }

    @Override void write(Sink sink) {
      sink.number(number);
    }
  }

  /** Value of a property or list element whose value is a boolean. */
  static class BooleanValue extends Value {
    private final boolean b;

    BooleanValue(boolean b) {
      this.b = b;
    }

    @Override void write(Sink sink) {
      sink.bool(b);
    }
  }

  /** Value of a property or list element whose value is a string. */
  static class StringValue extends Value {
    private final String s;

    StringValue(String s) {
      this.s = s;
    }

    @Override void write(Sink sink) {
      sink.string(s);
    }
  }

  /** Value of a property whose value is a code block. */
  static class CodeValue extends Value {
    private final String s;

    CodeValue(String s) {
      this.s = s;
    }

    @Override void write(Sink sink) {
      sink.code(s);
    }
  }

  /** Value of a property or list element whose value is a list. */
  static class ListValue extends Value {
    private final List<Value> list;

    ListValue(List<Value> list) {
      this.list = list;
    }

    @Override void write(Sink sink) {
      sink.list(list);
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

    @Override void write(Sink sink) {
      sink.obj(properties);
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

    @Override void write(Sink sink) {
      // Write "name { ... }"
      sink.identifier(name);
      sink.buf.append(' ');
      super.write(sink);
    }
  }

  /** Implementation of {@link net.hydromatic.filtex.lookml.ObjectHandler}
   * that writes LookML. */
  static class ObjectWriter implements ObjectHandler {
    final PairList<String, Value> properties = PairList.of();
    final Consumer<PairList<String, Value>> onClose;

    ObjectWriter(Consumer<PairList<String, Value>> onClose) {
      this.onClose = onClose;
    }

    @Override public ObjectWriter number(String property, Number value) {
      properties.add(property, new NumberValue(value));
      return this;
    }

    @Override public ObjectWriter bool(String property, boolean value) {
      properties.add(property, new BooleanValue(value));
      return this;
    }

    @Override public ObjectWriter string(String property, String value) {
      properties.add(property, new StringValue(value));
      return this;
    }

    @Override public ObjectWriter identifier(String property, String value) {
      properties.add(property, new IdentifierValue(value));
      return this;
    }

    @Override public ObjectWriter code(String property, String value) {
      properties.add(property, new CodeValue(value));
      return this;
    }

    @Override public ListHandlerImpl listOpen(String property) {
      return new ListHandlerImpl(list ->
          properties.add(property, new ListValue(list)));
    }

    @Override public ObjectHandler list(String property,
        Consumer<ListHandler> consumer) {
      ListHandlerImpl h = listOpen(property);
      consumer.accept(h);
      h.close();
      return this;
    }

    @Override public ObjectHandler objOpen(String property, String name) {
      return new ObjectWriter(properties ->
          this.properties.add(property,
              new NamedObjectValue(name, properties)));
    }

    @Override public ObjectHandler objOpen(String property) {
      return new ObjectWriter(properties ->
          this.properties.add(property, new ObjectValue(properties)));
    }

    @Override public ObjectHandler obj(String property, String name,
        Consumer<ObjectHandler> consumer) {
      final ObjectHandler h = objOpen(property, name);
      consumer.accept(h);
      h.close();
      return this;
    }

    @Override public ObjectHandler obj(String property,
        Consumer<ObjectHandler> consumer) {
      final ObjectHandler h = objOpen(property);
      consumer.accept(h);
      h.close();
      return this;
    }

    @Override public void close() {
      onClose.accept(properties);
    }
  }

  static class ListHandlerImpl implements ListHandler {
    final Consumer<List<Value>> onClose;
    final List<Value> list = new ArrayList<>();

    ListHandlerImpl(Consumer<List<Value>> onClose) {
      this.onClose = onClose;
    }

    @Override public ListHandler string(String value) {
      list.add(new StringValue(value));
      return this;
    }

    @Override public ListHandler number(Number value) {
      list.add(new NumberValue(value));
      return this;
    }

    @Override public ListHandler bool(boolean value) {
      list.add(new BooleanValue(value));
      return this;
    }

    @Override public ListHandler identifier(String value) {
      list.add(new IdentifierValue(value));
      return this;
    }

    @Override public ListHandler listOpen() {
      return new ListHandlerImpl(list -> this.list.add(new ListValue(list)));
    }

    @Override public ListHandler list(Consumer<ListHandler> consumer) {
      ListHandler h = listOpen();
      consumer.accept(h);
      h.close();
      return this;
    }

    @Override public void close() {
      onClose.accept(list);
    }
  }
}
