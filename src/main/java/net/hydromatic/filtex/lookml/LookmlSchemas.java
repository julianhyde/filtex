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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/** Utilities for {@link LookmlSchema}. */
public class LookmlSchemas {
  private LookmlSchemas() {}

  public static SchemaBuilder schemaBuilder() {
    return new SchemaBuilderImpl();
  }

  /** Builder for a {@link LookmlSchema}. */
  public interface SchemaBuilder {
    LookmlSchema build();

    /** Defines a {@link LookmlSchema.EnumType}
     * using an array of allowed values
     * and adds it to {@link LookmlSchema#enumTypes()}.
     *
     * <p>Can ignore return value because the enum type is available from
     * {@link SchemaBuilder#getEnumType(String)}. */
    @CanIgnoreReturnValue
    default SchemaBuilder addEnum(String name, String... allowedValues) {
      return addEnum(name, Arrays.asList(allowedValues));
    }

    /** Defines a {@link LookmlSchema.EnumType}
     * and adds it to {@link LookmlSchema#enumTypes()}.
     *
     * <p>Can ignore return value because the enum type is available from
     * {@link SchemaBuilder#getEnumType(String)}. */
    @CanIgnoreReturnValue
    SchemaBuilder addEnum(String name, Iterable<String> allowedValues);

    /** Defines an {@link LookmlSchema.ObjectType}
     * and adds it to {@link LookmlSchema#objectTypes()}. */
    SchemaBuilder addObjectType(String typeName,
        Function<ObjectTypeBuilder, LookmlSchema.ObjectType> action);

    /** Looks up an enum type; throws if not found. */
    LookmlSchema.EnumType getEnumType(String typeName);

    /** Looks up an object type; throws if not found. */
    LookmlSchema.ObjectType getObjectType(String typeName);

    /** Creates a property that wil appear in
     * {@link LookmlSchema#rootProperties()},
     * using an existing type named {@code propertyName}
     * from {@link #getObjectType(String)}. */
    SchemaBuilder addNamedObjectProperty(String propertyName);

    /** Creates a property that will appear in
     * {@link LookmlSchema#rootProperties()},
     * creating a new type named {@code propertyName}. */
    SchemaBuilder addNamedObjectProperty(String propertyName,
        Function<ObjectTypeBuilder, LookmlSchema.ObjectType> action);
  }

  /** Builder for {@link LookmlSchema.ObjectType}. */
  public interface ObjectTypeBuilder {
    /** Can ignore return value because the object type is available from
     * {@link SchemaBuilder#getObjectType(String)}. */
    @CanIgnoreReturnValue
    LookmlSchema.ObjectType build();

    /** Creates a property whose value is a number. */
    ObjectTypeBuilder addNumberProperty(String propertyName);

    /** Creates a property whose value is a string. */
    ObjectTypeBuilder addStringProperty(String propertyName);

    /** Creates a property whose value is a code block. */
    ObjectTypeBuilder addCodeProperty(String propertyName);

    /** Creates a property whose value is an identifier. */
    ObjectTypeBuilder addRefProperty(String propertyName);

    /** Creates a property whose value is an enum.
     *
     * <p>{@link SchemaBuilder#getEnumType(String)} must already
     * contain a type named {@code typeName}. */
    ObjectTypeBuilder addEnumProperty(String propertyName, String typeName);

    /** Creates an object property,
     * using an existing type named {@code propertyName}.
     *
     * <p>{@link SchemaBuilder#getObjectType(String)} must already
     * contain a type named {@code propertyName}. */
    ObjectTypeBuilder addObjectProperty(String propertyName);

    /** Creates an object property,
     * creating a new type named {@code propertyName}. */
    ObjectTypeBuilder addObjectProperty(String propertyName,
        Function<ObjectTypeBuilder, LookmlSchema.ObjectType> action);

    /** Creates a named-object property,
     * using an existing type named {@code propertyName}.
     *
     * <p>{@link SchemaBuilder#getObjectType(String)} must already
     * contain a type named {@code propertyName}. */
    ObjectTypeBuilder addNamedObjectProperty(String propertyName);

    /** Creates a named-object property,
     * creating a new type named {@code propertyName}. */
    ObjectTypeBuilder addNamedObjectProperty(String propertyName,
        Function<ObjectTypeBuilder, LookmlSchema.ObjectType> action);
  }

  /** Implementation of
   * {@link net.hydromatic.filtex.lookml.LookmlSchemas.SchemaBuilder}. */
  private static class SchemaBuilderImpl implements SchemaBuilder {
    final Map<String, PropertyImpl> rootPropertyMap = new LinkedHashMap<>();
    final Map<String, EnumTypeImpl> enumTypes = new LinkedHashMap<>();
    final Map<String, ObjectTypeImpl> objectTypes = new LinkedHashMap<>();

    @Override public LookmlSchema build() {
      // Deduce the set of 'code' properties
      final Set<String> codePropertyNames = new TreeSet<>();
      forEachProperty(property -> {
        if (property.type() == LookmlSchema.Type.CODE) {
          codePropertyNames.add(property.name());
        }
      });
      // Make sure no 'code' properties are used for non-code
      forEachProperty(property -> {
        if (property.type() != LookmlSchema.Type.CODE
            && codePropertyNames.contains(property.name())) {
          throw new IllegalArgumentException("property '" + property.name()
              + "' has both code and non-code uses");
        }
      });
      return new SchemaImpl(rootPropertyMap, objectTypes, enumTypes,
          codePropertyNames);
    }

    private void forEachProperty(Consumer<LookmlSchema.Property> consumer) {
      objectTypes.values().forEach(objectType ->
          objectType.propertyMap.values().forEach(consumer));
    }

    @Override public SchemaBuilder addEnum(String name,
        Iterable<String> allowedValues) {
      enumTypes.put(name, new EnumTypeImpl(allowedValues));
      return this;
    }

    @Override public SchemaBuilder addObjectType(String typeName,
        Function<ObjectTypeBuilder, LookmlSchema.ObjectType> action) {
      ObjectTypeBuilderImpl b = new ObjectTypeBuilderImpl(typeName, this);
      final ObjectTypeImpl objectType = (ObjectTypeImpl) action.apply(b);
      objectTypes.put(typeName, objectType);
      return this;
    }

    @Override public LookmlSchema.EnumType getEnumType(String typeName) {
      return requireNonNull(enumTypes.get(typeName), typeName);
    }

    @Override public LookmlSchema.ObjectType getObjectType(String typeName) {
      return requireNonNull(objectTypes.get(typeName), typeName);
    }

    @Override public SchemaBuilder addNamedObjectProperty(String propertyName) {
      // In LookML, an object property's type always has the same name as the
      // property.
      requireNonNull(objectTypes.get(propertyName), propertyName);
      rootPropertyMap.put(propertyName,
          new PropertyImpl(propertyName, LookmlSchema.Type.NAMED_OBJECT,
              propertyName));
      return this;
    }

    @Override public SchemaBuilder addNamedObjectProperty(String propertyName,
        Function<ObjectTypeBuilder, LookmlSchema.ObjectType> action) {
      // In LookML, an object property's type always has the same name as the
      // property.
      @SuppressWarnings("UnnecessaryLocalVariable")
      final String typeName = propertyName;
      ObjectTypeBuilder b = new ObjectTypeBuilderImpl(typeName, this);
      objectTypes.put(typeName, (ObjectTypeImpl) action.apply(b));

      return addNamedObjectProperty(propertyName);
    }
  }

  /** Implementation of {@link LookmlSchema}. */
  private static class SchemaImpl implements LookmlSchema {
    final Map<String, Property> rootPropertyMap;
    final Map<String, ObjectType> objectTypes;
    final Map<String, EnumType> enumTypes;
    final SortedSet<String> codePropertyNames;

    SchemaImpl(Map<String, PropertyImpl> rootPropertyMap,
        Map<String, ObjectTypeImpl> objectTypes,
        Map<String, EnumTypeImpl> enumTypes,
        Iterable<String> codePropertyNames) {
      this.rootPropertyMap = ImmutableMap.copyOf(rootPropertyMap);
      this.objectTypes = ImmutableMap.copyOf(objectTypes);
      this.enumTypes = ImmutableMap.copyOf(enumTypes);
      this.codePropertyNames = ImmutableSortedSet.copyOf(codePropertyNames);
    }

    @Override public Map<String, Property> rootProperties() {
      return rootPropertyMap;
    }

    @Override public Map<String, ObjectType> objectTypes() {
      return objectTypes;
    }

    @Override public Map<String, EnumType> enumTypes() {
      return enumTypes;
    }

    @Override public SortedSet<String> codePropertyNames() {
      return codePropertyNames;
    }
  }

  /** Implementation of
   * {@link net.hydromatic.filtex.lookml.LookmlSchema.ObjectType}. */
  static class ObjectTypeImpl implements LookmlSchema.ObjectType {
    final SortedMap<String, LookmlSchema.Property> propertyMap;

    ObjectTypeImpl(Map<String, PropertyImpl> propertyMap) {
      this.propertyMap = ImmutableSortedMap.copyOf(propertyMap);
    }

    @Override public SortedMap<String, LookmlSchema.Property> properties() {
      return propertyMap;
    }
  }

  /** Implementation of
   * {@link net.hydromatic.filtex.lookml.LookmlSchema.EnumType}. */
  static class EnumTypeImpl implements LookmlSchema.EnumType {
    final SortedSet<String> values;

    EnumTypeImpl(Iterable<String> values) {
      // Sort values alphabetically, eliminate duplicates, check for nulls.
      this.values = ImmutableSortedSet.copyOf(values);
    }

    @Override public SortedSet<String> allowedValues() {
      return values;
    }
  }

  /** Implementation of
   * {@link net.hydromatic.filtex.lookml.LookmlSchema.Property}. */
  static class PropertyImpl implements LookmlSchema.Property {
    private final String name;
    private final LookmlSchema.Type type;
    private final String typeName;

    PropertyImpl(String name, LookmlSchema.Type type, String typeName) {
      this.name = name;
      this.type = type;
      this.typeName = typeName;
    }

    @Override public String name() {
      return name;
    }

    @Override public LookmlSchema.Type type() {
      return type;
    }

    @Override public String typeName() {
      return typeName;
    }
  }

  /** Implementation of {@link ObjectTypeBuilder}. */
  static class ObjectTypeBuilderImpl implements ObjectTypeBuilder {
    final String name;
    final SchemaBuilderImpl schemaBuilder;
    final Map<String, PropertyImpl> propertyMap = new LinkedHashMap<>();

    ObjectTypeBuilderImpl(String name, SchemaBuilderImpl schemaBuilder) {
      this.name = name;
      this.schemaBuilder = schemaBuilder;
    }

    @Override public LookmlSchema.ObjectType build() {
      ObjectTypeImpl objectType = new ObjectTypeImpl(propertyMap);
      schemaBuilder.objectTypes.put(name, objectType);
      return objectType;
    }

    @Override public ObjectTypeBuilder addNumberProperty(String propertyName) {
      return addProperty_(propertyName, LookmlSchema.Type.NUMBER, "");
    }

    @Override public ObjectTypeBuilder addStringProperty(String propertyName) {
      return addProperty_(propertyName, LookmlSchema.Type.STRING, "");
    }

    @Override public ObjectTypeBuilder addCodeProperty(String propertyName) {
      return addProperty_(propertyName, LookmlSchema.Type.CODE, "");
    }

    @Override public ObjectTypeBuilder addRefProperty(String propertyName) {
      return addProperty_(propertyName, LookmlSchema.Type.REF, "");
    }

    @Override public ObjectTypeBuilder addEnumProperty(String propertyName,
        String typeName) {
      return addProperty_(propertyName, LookmlSchema.Type.ENUM, typeName);
    }

    @Override public ObjectTypeBuilder addObjectProperty(String propertyName) {
      return addProperty_(propertyName, LookmlSchema.Type.OBJECT, propertyName);
    }

    @Override public ObjectTypeBuilder addObjectProperty(String propertyName,
        Function<ObjectTypeBuilder, LookmlSchema.ObjectType> action) {
      final ObjectTypeBuilderImpl b =
          new ObjectTypeBuilderImpl(propertyName, schemaBuilder);
      final ObjectTypeImpl objectType = (ObjectTypeImpl) action.apply(b);
      schemaBuilder.objectTypes.put(propertyName, objectType);
      return addProperty_(propertyName, LookmlSchema.Type.OBJECT, propertyName);
    }

    @Override public ObjectTypeBuilder addNamedObjectProperty(
        String propertyName) {
      return addProperty_(propertyName, LookmlSchema.Type.NAMED_OBJECT,
          propertyName);
    }

    @Override public ObjectTypeBuilder addNamedObjectProperty(
        String propertyName,
        Function<ObjectTypeBuilder, LookmlSchema.ObjectType> action) {
      // In LookML, an object property's type always has the same name as the
      // property.
      final ObjectTypeBuilderImpl b =
          new ObjectTypeBuilderImpl(propertyName, schemaBuilder);
      final ObjectTypeImpl objectType = (ObjectTypeImpl) action.apply(b);
      schemaBuilder.objectTypes.put(propertyName, objectType);
      return addProperty_(propertyName, LookmlSchema.Type.NAMED_OBJECT,
          propertyName);
    }

    private ObjectTypeBuilderImpl addProperty_(String propertyName,
        LookmlSchema.Type type, String typeName) {
      switch (type) {
      case ENUM:
        requireNonNull(schemaBuilder.enumTypes.get(typeName), typeName);
        break;
      case OBJECT:
      case NAMED_OBJECT:
        requireNonNull(schemaBuilder.objectTypes.get(propertyName), typeName);
        break;
      default:
        if (!typeName.isEmpty()) {
          throw new IllegalArgumentException(typeName);
        }
      }
      propertyMap.put(propertyName,
          new PropertyImpl(propertyName, type, typeName));
      return this;
    }
  }
}

// End LookmlSchemas.java
