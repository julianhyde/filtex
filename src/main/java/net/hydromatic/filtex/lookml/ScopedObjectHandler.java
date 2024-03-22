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

import java.util.Map;
import java.util.function.Consumer;

abstract class ScopedObjectHandler extends LaxHandlers.ObjectBuilder {
  protected final Map<String, LookmlSchema.Property> propertyMap;
  protected final Consumer<Object> consumer;

  private ScopedObjectHandler(Map<String, LookmlSchema.Property> propertyMap,
      Consumer<Object> consumer) {
    super(pairList -> { });
    this.propertyMap = propertyMap;
    this.consumer = consumer;
  }

  /** Creates a scoped handler. */
  static ScopedObjectHandler create(LookmlSchema schema,
      PolyBuilder polyBuilder, Consumer<Object> consumer) {
    return new RootScopedHandler(schema, polyBuilder, consumer);
  }

  /** Scoped handler that is at the root of the tree. */
  static class RootScopedHandler extends ScopedObjectHandler {
    final LookmlSchema schema;
    final PolyBuilder polyBuilder;

    RootScopedHandler(LookmlSchema schema, PolyBuilder polyBuilder,
        Consumer<Object> consumer) {
      super(schema.rootProperties(), consumer);
      this.schema = schema;
      this.polyBuilder = polyBuilder;
    }

    @Override public ObjectHandler objOpen(String propertyName, String name) {
      final LookmlSchema.Property property = propertyMap.get(propertyName);
      final LookmlSchema.ObjectType objectType =
          schema.objectTypes().get(property.typeName());
      return new NonRootScopedHandler(this, propertyName, objectType, name,
          consumer);
    }

    @Override public ObjectHandler objOpen(String propertyName) {
      final LookmlSchema.Property property = propertyMap.get(propertyName);
      final LookmlSchema.ObjectType objectType =
          schema.objectTypes().get(property.typeName());
      return new NonRootScopedHandler(this, propertyName, objectType, "",
          consumer);
    }
  }

  /** Scoped handler that is not at the root of the tree. */
  static class NonRootScopedHandler extends ScopedObjectHandler {
    private final RootScopedHandler root;
    private final String typeName;
    private final LookmlSchema.ObjectType type;
    private final String name;

    NonRootScopedHandler(RootScopedHandler root, String typeName,
        LookmlSchema.ObjectType type, String name, Consumer<Object> consumer) {
      super(type.properties(), consumer);
      this.root = root;
      this.typeName = typeName;
      this.type = type;
      this.name = name;
    }

    @Override public ObjectHandler objOpen(String propertyName, String name) {
      final LookmlSchema.Property property = propertyMap.get(propertyName);
      final LookmlSchema.ObjectType objectType =
          root.schema.objectTypes().get(property.typeName());
      return new NonRootScopedHandler(root, propertyName, objectType, name,
          o -> properties.add(propertyName, Values.wrapped(o)));
    }

    @Override public ObjectHandler objOpen(String propertyName) {
      final LookmlSchema.Property property = propertyMap.get(propertyName);
      final LookmlSchema.ObjectType objectType =
          root.schema.objectTypes().get(property.typeName());
      return new NonRootScopedHandler(root, propertyName, objectType, "",
          o -> properties.add(propertyName, Values.wrapped(o)));
    }

    @Override public void close() {
      final Object o =
          root.polyBuilder.build(typeName, type, name, this.properties);
      consumer.accept(o);
    }
  }

  /** Can build an object of any type. */
  public interface PolyBuilder {
    Object build(String typeName, LookmlSchema.ObjectType objectType,
        String name, PairList<String, Value> properties);
  }
}

// End ScopedObjectHandler.java
