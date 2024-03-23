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

import net.hydromatic.filtex.util.PairList;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Various implementations of {@link ObjectHandler} */
public class LaxHandlers {
  private LaxHandlers() {}

  /** Creates a writer.
   *
   * @param buf String builder to which to write the LookML
   * @param offset Number of spaces to increase indentation each time we enter
   *              a nested object or list
   * @param pretty Whether to pretty-print (with newlines and indentation) */
  public static ObjectHandler writer(StringBuilder buf, int offset,
      boolean pretty) {
    final LookmlWriter sink = new LookmlWriter(pretty, buf, offset);
    return build(sink::propertyList);
  }

  /** Creates a writer.
   *
   * @param buf String builder to which to write the LookML
   * @param offset Number of spaces to increase indentation each time we enter
   *              a nested object or list
   * @param pretty Whether to pretty-print (with newlines and indentation) */
  public static PropertyHandler writerTyped(StringBuilder buf, int offset,
      boolean pretty) {
    return untype(writer(buf, offset, pretty));
  }

  /** Creates a handler that writes each event, as a string, to a consumer. */
  public static ObjectHandler logger(Consumer<String> list) {
    return LoggingHandler.create(list);
  }

  /** Creates a handler that writes each event, as a string, to a consumer. */
  public static PropertyHandler loggerTyped(Consumer<String> list) {
    return new LoggingPropertyHandler(list);
  }

  /** Creates a handler that writes each error event, as a string,
   *  to a consumer. */
  public static ErrorHandler errorLogger(Consumer<String> list) {
    return new LoggingErrorHandler(list);
  }

  /** Creates a property handler that writes to an object handler. */
  public static PropertyHandler untype(ObjectHandler consumer) {
    return new UntypingHandler(consumer);
  }

  /** Creates a handler that writes each event, as a string, to a consumer. */
  public static ObjectHandler filter(ObjectHandler consumer) {
    return new FilterHandler(consumer);
  }

  /** Creates a handler that validates each event against a
   * {@link LookmlSchema}. */
  public static ObjectHandler validator(PropertyHandler consumer,
      LookmlSchema schema, ErrorHandler errorHandler) {
    return ValidatingHandler.create(schema, consumer, errorHandler);
  }

  /** Creates a handler that validates each event against a
   * {@link LookmlSchema} and writes to an {@link ObjectHandler} consumer. */
  public static ObjectHandler validator(ObjectHandler consumer,
      LookmlSchema schema, ErrorHandler errorHandler) {
    PropertyHandler propertyHandler = untype(consumer);
    return ValidatingHandler.create(schema, propertyHandler, errorHandler);
  }

  /** Checks whether the input contains at least one instance of every
   * property. */
  public static PropertyHandler completenessChecker(LookmlSchema schema,
      Consumer<LookmlSchema.Property> propertiesSeen) {
    return new RootCompletenessChecker(schema, propertiesSeen);
  }

  /** Creates a list handler that swallows all events. */
  public static ListHandler nullListHandler() {
    return NullListHandler.INSTANCE;
  }

  public static ObjectHandler nullObjectHandler() {
    return NullObjectHandler.INSTANCE;
  }

  /** Creates an ObjectHandler that converts events into a document. */
  public static ObjectHandler build(
      Consumer<PairList<String, Value>> consumer) {
    return new ObjectBuilder(consumer);
  }

  /** Implementation of {@link ObjectHandler}
   * that builds a list of properties,
   * then calls a consumer on the completed list. */
  static class ObjectBuilder implements ObjectHandler {
    final PairList<String, Value> properties = PairList.of();
    final Consumer<PairList<String, Value>> onClose;

    ObjectBuilder(Consumer<PairList<String, Value>> onClose) {
      this.onClose = onClose;
    }

    @Override public ObjectBuilder comment(String comment) {
      // ignore comment
      return this;
    }

    @Override public ObjectBuilder number(String propertyName, Number value) {
      properties.add(propertyName, Values.number(value));
      return this;
    }

    @Override public ObjectBuilder bool(String propertyName, boolean value) {
      properties.add(propertyName, Values.bool(value));
      return this;
    }

    @Override public ObjectBuilder string(String propertyName, String value) {
      properties.add(propertyName, Values.string(value));
      return this;
    }

    @Override public ObjectBuilder identifier(String propertyName,
        String value) {
      properties.add(propertyName, Values.identifier(value));
      return this;
    }

    @Override public ObjectBuilder code(String propertyName, String value) {
      properties.add(propertyName, Values.code(value));
      return this;
    }

    @Override public ListBuilder listOpen(String propertyName) {
      return new ListBuilder(list ->
          properties.add(propertyName, Values.list(list)));
    }

    @Override public ObjectHandler objOpen(String propertyName, String name) {
      return new ObjectBuilder(properties ->
          this.properties.add(propertyName,
              Values.namedObject(name, properties)));
    }

    @Override public ObjectHandler objOpen(String property) {
      return new ObjectBuilder(properties ->
          this.properties.add(property,
              Values.object(properties)));
    }

    @Override public void close() {
      onClose.accept(properties);
    }
  }

  /** Implementation of {@link ListHandler}
   * that builds a list of values,
   * then calls a consumer when done. */
  static class ListBuilder implements ListHandler {
    final Consumer<List<Value>> onClose;
    final List<Value> list = new ArrayList<>();

    ListBuilder(Consumer<List<Value>> onClose) {
      this.onClose = onClose;
    }

    @Override public ListHandler string(String value) {
      list.add(Values.string(value));
      return this;
    }

    @Override public ListHandler number(Number value) {
      list.add(Values.number(value));
      return this;
    }

    @Override public ListHandler bool(boolean value) {
      list.add(Values.bool(value));
      return this;
    }

    @Override public ListHandler identifier(String value) {
      list.add(Values.identifier(value));
      return this;
    }

    @Override public ListHandler pair(String ref, String identifier) {
      list.add(Values.pair(ref, identifier));
      return this;
    }

    @Override public ListHandler comment(String comment) {
      // Ignore the comment
      return this;
    }

    @Override public ListHandler listOpen() {
      return new ListBuilder(list -> this.list.add(Values.list(list)));
    }

    @Override public void close() {
      onClose.accept(list);
    }
  }

  /** Implementation of {@link net.hydromatic.filtex.lookml.ListHandler}
   * that discards all events. */
  enum NullListHandler implements ListHandler {
    INSTANCE;

    @Override public ListHandler string(String value) {
      return this;
    }

    @Override public ListHandler number(Number value) {
      return this;
    }

    @Override public ListHandler bool(boolean value) {
      return this;
    }

    @Override public ListHandler identifier(String value) {
      return this;
    }

    @Override public ListHandler pair(String ref, String identifier) {
      return this;
    }

    @Override public ListHandler comment(String comment) {
      return this;
    }

    @Override public void close() {
    }

    @Override public ListHandler listOpen() {
      return this; // no point creating another instance
    }
  }

  /** Implementation of {@link net.hydromatic.filtex.lookml.ObjectHandler}
   * that discards all events. */
  enum NullObjectHandler implements ObjectHandler {
    INSTANCE;

    @Override public ObjectHandler number(String propertyName, Number value) {
      return this;
    }

    @Override public ObjectHandler bool(String propertyName, boolean value) {
      return this;
    }

    @Override public ObjectHandler string(String propertyName, String value) {
      return this;
    }

    @Override public ObjectHandler identifier(String propertyName,
        String value) {
      return this;
    }

    @Override public ObjectHandler code(String propertyName, String value) {
      return this;
    }

    @Override public ListHandler listOpen(String propertyName) {
      return NullListHandler.INSTANCE;
    }

    @Override public ObjectHandler objOpen(String property) {
      return this;
    }

    @Override public ObjectHandler objOpen(String propertyName, String name) {
      return this;
    }

    @Override public void close() {
    }

    @Override public ObjectHandler comment(String comment) {
      return this;
    }
  }

  /** Implementation of {@link PropertyHandler}
   * that writes to an {@link ObjectHandler}.
   *
   * <p>This allows you to reverse the effects of schema validation, and go
   * from typed to untyped events. Raw values are given {@link Value} wrappers,
   * so that they can be interpreted correctly (say as string, identifier or
   * enum) without accompanying properties.
   */
  private static class UntypingHandler implements PropertyHandler {
    private final ObjectHandler consumer;

    UntypingHandler(ObjectHandler consumer) {
      this.consumer = consumer;
    }

    @Override public PropertyHandler property(LookmlSchema.Property property,
        Object value) {
      switch (property.type()) {
      case STRING:
        consumer.string(property.name(), (String) value);
        break;
      case CODE:
        consumer.code(property.name(), (String) value);
        break;
      case NUMBER:
        consumer.number(property.name(), (Number) value);
        break;
      case ENUM:
      case REF:
        consumer.identifier(property.name(), (String) value);
        break;
      case REF_LIST:
        final ListHandler listHandler = consumer.listOpen(property.name());
        ((List<String>) value).forEach(listHandler::identifier);
        break;
      default:
        throw new AssertionError(property.type());
      }
      return this;
    }

    @Override public ListHandler listOpen(LookmlSchema.Property property) {
      return consumer.listOpen(property.name());
    }

    @Override public PropertyHandler objOpen(LookmlSchema.Property property) {
      final ObjectHandler subConsumer = consumer.objOpen(property.name());
      return new UntypingHandler(subConsumer);
    }

    @Override public PropertyHandler objOpen(LookmlSchema.Property property,
        String name) {
      final ObjectHandler subConsumer = consumer.objOpen(property.name(), name);
      return new UntypingHandler(subConsumer);
    }

    @Override public void close() {
      consumer.close();
    }
  }

  /** Property handler that notes each property seen while traversing a
   * document, and at the end outputs all properties in the schema that were
   * never seen. */
  private static class CompletenessChecker implements PropertyHandler {
    final Set<LookmlSchema.Property> propertiesSeen;

    private CompletenessChecker(Set<LookmlSchema.Property> propertiesSeen) {
      this.propertiesSeen = propertiesSeen;
    }

    @Override public PropertyHandler property(LookmlSchema.Property property,
        Object value) {
      propertiesSeen.add(property);
      return this;
    }

    @Override public ListHandler listOpen(LookmlSchema.Property property) {
      propertiesSeen.add(property);
      return nullListHandler(); // we don't care about list elements
    }

    @Override public PropertyHandler objOpen(LookmlSchema.Property property) {
      propertiesSeen.add(property);
      return new CompletenessChecker(propertiesSeen);
    }

    @Override public PropertyHandler objOpen(LookmlSchema.Property property,
        String name) {
      propertiesSeen.add(property);
      return new CompletenessChecker(propertiesSeen);
    }

    @Override public void close() {
      // nothing to do
    }
  }

  /** The root of a tree of {@link CompletenessChecker}. It contains state
   * shared with the whole tree, and outputs not-seen properties on close. */
  private static class RootCompletenessChecker extends CompletenessChecker {
    final LookmlSchema schema;
    final Consumer<LookmlSchema.Property> propertiesSeenConsumer;

    private RootCompletenessChecker(LookmlSchema schema,
        Consumer<LookmlSchema.Property> propertiesSeenConsumer) {
      super(new LinkedHashSet<>());
      this.schema = schema;
      this.propertiesSeenConsumer = propertiesSeenConsumer;
    }

    @Override public void close() {
      propertiesSeen.forEach(propertiesSeenConsumer);
    }
  }

}
