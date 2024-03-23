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

abstract class ScopedObjectHandler extends LaxHandlers.ObjectBuilder {
  protected final Map<String, LookmlSchema.Property> propertyMap;
  protected final ObjectConsumer consumer;

  private ScopedObjectHandler(Map<String, LookmlSchema.Property> propertyMap,
      ObjectConsumer consumer) {
    super(pairList -> { });
    this.propertyMap = propertyMap;
    this.consumer = consumer;
  }

  /** Creates a scoped handler. */
  static ScopedObjectHandler create(LookmlSchema schema,
      ObjectConsumer consumer) {
    return new RootScopedHandler(schema, consumer);
  }

  /** Scoped handler that is at the root of the tree. */
  static class RootScopedHandler extends ScopedObjectHandler {
    final LookmlSchema schema;

    RootScopedHandler(LookmlSchema schema, ObjectConsumer consumer) {
      super(schema.rootProperties(), consumer);
      this.schema = schema;
    }

    @Override public ObjectHandler objOpen(String propertyName, String name) {
      final LookmlSchema.Property property = propertyMap.get(propertyName);
      final LookmlSchema.ObjectType objectType =
          schema.objectTypes().get(property.typeName());
      return new NonRootScopedHandler(this, propertyName, objectType, name,
          consumer.child());
    }

    @Override public ObjectHandler objOpen(String propertyName) {
      final LookmlSchema.Property property = propertyMap.get(propertyName);
      final LookmlSchema.ObjectType objectType =
          schema.objectTypes().get(property.typeName());
      return new NonRootScopedHandler(this, propertyName, objectType, "",
          consumer.child());
    }
  }

  /** Scoped handler that is not at the root of the tree. */
  static class NonRootScopedHandler extends ScopedObjectHandler {
    private final RootScopedHandler root;
    private final String typeName;
    private final LookmlSchema.ObjectType type;
    private final String name;

    NonRootScopedHandler(RootScopedHandler root, String typeName,
        LookmlSchema.ObjectType type, String name, ObjectConsumer consumer) {
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
          consumer.child());
    }

    @Override public ObjectHandler objOpen(String propertyName) {
      final LookmlSchema.Property property = propertyMap.get(propertyName);
      final LookmlSchema.ObjectType objectType =
          root.schema.objectTypes().get(property.typeName());
      return new NonRootScopedHandler(root, propertyName, objectType, "",
          consumer.child());
    }

    @Override public void close() {
      consumer.accept(typeName, type, name, null);
    }
  }

  /** Can build an object of any type. */
  public interface ObjectConsumer {
    /** Accepts an object. */
    void accept(String typeName, LookmlSchema.ObjectType objectType,
        String name, PairList<String, Object> properties);

    /** Returns an ObjectConsumer that will write into this one. */
    ObjectConsumer child();
  }
}

// End ScopedObjectHandler.java
