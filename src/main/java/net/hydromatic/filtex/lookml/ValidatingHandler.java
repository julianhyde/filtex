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

import com.google.common.collect.ImmutableSet;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import static com.google.common.base.Preconditions.checkArgument;

import static java.util.Objects.requireNonNull;

/** Handler that validates a document against a schema.
 *
 * <p>The document is represented by a stream of LookML parse events sent
 * as calls to the {@link ObjectHandler} interface. After validation, the
 * events are sent to a consuming {@code ObjectHandler}.
 *
 * <p>The schema is represented as a {@link LookmlSchema}. */
abstract class ValidatingHandler extends FilterHandler {
  protected final Map<String, LookmlSchema.Property> propertyMap;

  ValidatingHandler(ObjectHandler consumer,
      Map<String, LookmlSchema.Property> propertyMap) {
    super(consumer);
    this.propertyMap = propertyMap;
  }

  /** Creates a validating handler. */
  static ObjectHandler create(LookmlSchema schema,
      ObjectHandler consumer, ErrorHandler errorHandler) {
    return new RootValidatingHandler(consumer, schema, errorHandler);
  }

  /** Returns whether we can assign a value of {@code type}
   * to a given {@code property}.
   *
   * <p>For example, if the property has type {@link LookmlSchema.Type#NUMBER}
   * we can only assign values of type {@code NUMBER} property.
   * If a property has type {@link LookmlSchema.Type#ENUM} we can assign values
   * of type {@link LookmlSchema.Type#REF}, but we must ensure that they are
   * valid for the {@link LookmlSchema.EnumType}. */
  private static boolean canAssign(LookmlSchema.Property property,
      LookmlSchema.Type type) {
    return property.type() == type
        || property.type() == LookmlSchema.Type.ENUM
        && type == LookmlSchema.Type.REF;
  }

  /** Handler for validating an object that is not at the root of the
   * document. */
  private static class NonRootValidatingHandler extends ValidatingHandler {
    private final RootValidatingHandler root;

    /** Name of the enclosing object type. */
    final String parentTypeName;


    NonRootValidatingHandler(ObjectHandler objectHandler,
        RootValidatingHandler root, String parentTypeName,
        SortedMap<String, LookmlSchema.Property> propertyMap) {
      super(objectHandler, propertyMap);
      this.root = root;
      this.parentTypeName = parentTypeName;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean propertyIsValid(String propertyName,
        LookmlSchema.@Nullable Property property, LookmlSchema.Type type) {
      if (property == null) {
        root.errorHandler.invalidPropertyOfParent(propertyName, parentTypeName);
        return false;
      }
      if (!canAssign(property, type)) {
        root.errorHandler.invalidPropertyType(propertyName, property.type(),
            type);
        return false;
      }
      return true; // property is valid
    }

    @Override public ObjectHandler number(String propertyName, Number value) {
      final LookmlSchema.Property property = propertyMap.get(propertyName);
      if (!propertyIsValid(propertyName, property, LookmlSchema.Type.NUMBER)) {
        return this;
      }
      return super.number(propertyName, value);
    }

    @Override public ObjectHandler string(String propertyName, String value) {
      final LookmlSchema.Property property = propertyMap.get(propertyName);
      if (!propertyIsValid(propertyName, property, LookmlSchema.Type.STRING)) {
        return this;
      }
      return super.string(propertyName, value);
    }

    @Override public ObjectHandler bool(String propertyName, boolean value) {
      final LookmlSchema.Property property = propertyMap.get(propertyName);
      if (property == null) {
        root.errorHandler.invalidPropertyOfParent(propertyName, parentTypeName);
        return this;
      }
      if (property.type() != LookmlSchema.Type.ENUM) {
        root.errorHandler.invalidPropertyType(propertyName, property.type(),
            LookmlSchema.Type.ENUM);
        return this;
      }
      if (!root.probableBooleanTypes.contains(property.typeName())) {
        root.errorHandler.invalidEnumValue(parentTypeName, propertyName,
            property.typeName(), value ? "true" : "false");
      }
      return super.bool(propertyName, value);
    }

    @Override public ObjectHandler code(String propertyName, String value) {
      final LookmlSchema.Property property = propertyMap.get(propertyName);
      if (!propertyIsValid(propertyName, property, LookmlSchema.Type.CODE)) {
        return this;
      }
      return super.code(propertyName, value);
    }

    @Override public ObjectHandler identifier(String propertyName,
        String value) {
      final LookmlSchema.Property property = propertyMap.get(propertyName);
      if (property == null) {
        root.errorHandler.invalidPropertyOfParent(propertyName,
            parentTypeName);
        return this;
      }
      final LookmlSchema.Type propertyType = property.type();
      if (propertyType != LookmlSchema.Type.REF
          && propertyType != LookmlSchema.Type.ENUM) {
        root.errorHandler.invalidPropertyType(propertyName, propertyType,
            LookmlSchema.Type.REF);
        return this;
      }
      if (propertyType == LookmlSchema.Type.ENUM) {
        LookmlSchema.EnumType enumType =
            requireNonNull(root.schema.enumTypes().get(property.typeName()));
        if (!enumType.allowedValues().contains(value)) {
          root.errorHandler.invalidEnumValue(parentTypeName, propertyName,
              property.typeName(), value);
          return this;
        }
      }
      return super.identifier(propertyName, value);
    }

    @Override public ObjectHandler objOpen(String propertyName) {
      final LookmlSchema.Property property = propertyMap.get(propertyName);
      if (!propertyIsValid(propertyName, property, LookmlSchema.Type.OBJECT)) {
        return LaxHandlers.nullObjectHandler();
      }
      final ObjectHandler objectHandler = consumer.objOpen(propertyName);
      final LookmlSchema.ObjectType objectType =
          root.schema.objectTypes().get(propertyName);
      return new NonRootValidatingHandler(objectHandler, root, propertyName,
          objectType.properties());
    }

    @Override public ObjectHandler objOpen(String propertyName, String name) {
      final LookmlSchema.Property property = propertyMap.get(propertyName);
      if (!propertyIsValid(propertyName, property,
          LookmlSchema.Type.NAMED_OBJECT)) {
        return LaxHandlers.nullObjectHandler();
      }
      final ObjectHandler objectHandler = consumer.objOpen(propertyName, name);
      final LookmlSchema.ObjectType objectType =
          root.schema.objectTypes().get(propertyName);
      return new NonRootValidatingHandler(objectHandler, root, propertyName,
          objectType.properties());
    }

    @Override public ListHandler listOpen(String propertyName) {
      final LookmlSchema.Property property = propertyMap.get(propertyName);
      if (property == null) {
        root.errorHandler.invalidPropertyOfParent(propertyName, parentTypeName);
        return LaxHandlers.nullListHandler();
      } else if (property.type() != LookmlSchema.Type.REF_LIST
          && property.type() != LookmlSchema.Type.REF_STRING_MAP
          && property.type() != LookmlSchema.Type.STRING_LIST) {
        root.errorHandler.invalidPropertyType(propertyName, property.type(),
            LookmlSchema.Type.REF_LIST);
        return LaxHandlers.nullListHandler();
      }
      final ListHandler listHandler = consumer.listOpen(propertyName);
      return new ValidatingListHandler(listHandler, root, property);
    }
  }

  /** Implementation of {@link net.hydromatic.filtex.lookml.ValidatingHandler}
   * that stores the common data in a tree of handlers. */
  private static class RootValidatingHandler extends ValidatingHandler {
    private final LookmlSchema schema;
    private final ErrorHandler errorHandler;
    /** Set of enum type names that look like boolean (have allowable values
     * yes and no or true and false). */
    final Set<String> probableBooleanTypes;

    RootValidatingHandler(ObjectHandler consumer, LookmlSchema schema,
        ErrorHandler errorHandler) {
      super(consumer, schema.rootProperties());
      this.schema = requireNonNull(schema, "schema");
      this.errorHandler = requireNonNull(errorHandler, "errorHandler");
      this.probableBooleanTypes =
          schema.enumTypes().entrySet().stream()
              .filter(e -> isBooleanValueSet(e.getValue().allowedValues()))
              .map(Map.Entry::getKey)
              .collect(ImmutableSet.toImmutableSet());
    }

    private static boolean isBooleanValueSet(Set<String> valueSet) {
      return valueSet.contains("true") && valueSet.contains("false")
          || valueSet.contains("yes") && valueSet.contains("no");
    }

    @Override public ObjectHandler number(String propertyName, Number value) {
      errorHandler.invalidRootProperty(propertyName);
      return this;
    }

    @Override public ObjectHandler bool(String propertyName, boolean value) {
      errorHandler.invalidRootProperty(propertyName);
      return this;
    }

    @Override public ObjectHandler string(String propertyName, String value) {
      errorHandler.invalidRootProperty(propertyName);
      return this;
    }

    @Override public ObjectHandler identifier(String propertyName,
        String value) {
      errorHandler.invalidRootProperty(propertyName);
      return this;
    }

    @Override public ObjectHandler code(String propertyName, String value) {
      errorHandler.invalidRootProperty(propertyName);
      return this;
    }

    @Override public ListHandler listOpen(String propertyName) {
      errorHandler.invalidRootProperty(propertyName);
      return LaxHandlers.nullListHandler();
    }

    @Override public ObjectHandler objOpen(String propertyName) {
      final LookmlSchema.Property property =
          schema.rootProperties().get(propertyName);
      if (property != null
          && property.type() == LookmlSchema.Type.NAMED_OBJECT) {
        errorHandler.nameRequired(propertyName);
      } else {
        errorHandler.invalidRootProperty(propertyName);
      }
      return LaxHandlers.nullObjectHandler();
    }

    @Override public ObjectHandler objOpen(String propertyName, String name) {
      final LookmlSchema.Property property = propertyMap.get(propertyName);
      if (property == null) {
        errorHandler.invalidRootProperty(propertyName);
        return LaxHandlers.nullObjectHandler();
      }
      final ObjectHandler objectHandler = consumer.objOpen(propertyName, name);
      final LookmlSchema.ObjectType objectType =
          schema.objectTypes().get(propertyName);
      return new NonRootValidatingHandler(objectHandler, this, propertyName,
          objectType.properties());
    }
  }

  /** Implementation of {@link net.hydromatic.filtex.lookml.ListHandler}
   * that validates each element of a list. */
  private static class ValidatingListHandler extends FilterListHandler {
    private final RootValidatingHandler root;
    private final LookmlSchema.Property property;

    ValidatingListHandler(ListHandler listHandler, RootValidatingHandler root,
        LookmlSchema.Property property) {
      super(listHandler);
      this.root = root;
      this.property = property;
      checkArgument(property.type() == LookmlSchema.Type.REF_LIST
          || property.type() == LookmlSchema.Type.STRING_LIST
          || property.type() == LookmlSchema.Type.REF_STRING_MAP);
    }

    @Override public ListHandler string(String value) {
      if (property.type() != LookmlSchema.Type.STRING_LIST) {
        root.errorHandler.invalidListElement(property.name(),
            LookmlSchema.Type.STRING, property.type());
        return this; // skip the element
      }
      return super.string(value);
    }

    @Override public ListHandler number(Number value) {
      // Currently, there is no type of list whose elements are numbers.
      root.errorHandler.invalidListElement(property.name(),
          LookmlSchema.Type.NUMBER, property.type());
      return this; // skip the element
    }

    @Override public ListHandler bool(boolean value) {
      // Currently, there is no type of list whose elements are booleans.
      root.errorHandler.invalidListElement(property.name(),
          LookmlSchema.Type.ENUM, property.type());
      return this; // skip the element
    }

    @Override public ListHandler identifier(String value) {
      if (property.type() != LookmlSchema.Type.REF_LIST) {
        root.errorHandler.invalidListElement(property.name(),
            LookmlSchema.Type.REF, property.type());
        return this; // skip the element
      }
      return super.identifier(value);
    }

    @Override public ListHandler pair(String ref, String identifier) {
      if (property.type() != LookmlSchema.Type.REF_STRING_MAP) {
        root.errorHandler.invalidListElement(property.name(),
            LookmlSchema.Type.REF, property.type());
        return this; // skip the element
      }
      return super.pair(ref, identifier);
    }

    @Override public ListHandler listOpen() {
      // Currently, there is no type of list whose elements are lists.
      root.errorHandler.invalidListElement(property.name(),
          LookmlSchema.Type.REF_LIST, property.type());
      return LaxHandlers.nullListHandler(); // skip the element
    }
  }
}