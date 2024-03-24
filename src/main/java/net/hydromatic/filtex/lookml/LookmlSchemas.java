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

import com.google.common.base.Charsets;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;

import static java.util.Objects.requireNonNull;

/** Utilities for {@link LookmlSchema}. */
public class LookmlSchemas {
  /** Cached schema schema. */
  private static final Supplier<LookmlSchema> SCHEMA_SCHEMA_SUPPLIER =
      Suppliers.memoize(LookmlSchemas::schemaSchema_);

  private LookmlSchemas() {}

  /** Creates a SchemaBuilder. */
  public static SchemaBuilder schemaBuilder() {
    return new SchemaBuilderImpl();
  }

  /** Loads a schema from a file in Schema LookML format. */
  public static LookmlSchema load(URL url, @Nullable LookmlSchema validate) {
    final String s = urlContents(url);

    // Parse the string into an AST
    final List<PairList<String, Value>> list = new ArrayList<>();
    final List<String> errorList;
    ObjectHandler handler = LaxHandlers.build(list::add);
    if (validate != null) {
      errorList = new ArrayList<>();
      handler =
          LaxHandlers.validator(handler, validate,
              LaxHandlers.errorLogger(errorList::add));
    } else {
      errorList = ImmutableList.of();
    }
    LaxParser.parse(handler, ImmutableList.of(), s);
    handler.close();
    if (!errorList.isEmpty()) {
      throw new IllegalArgumentException("invalid: " + errorList);
    }

    final SchemaBuilder b = schemaBuilder();
    new AstWalker(b).accept(list.get(0));
    return b.build();
  }

  /** Returns the contents of a URL.
   *
   * <p>We can obsolete this method when we have a way to invoke the parser
   * on multiple sources (strings, files, URLs). */
  public static String urlContents(URL url) {
    try (InputStream stream = url.openStream();
         Reader r = new InputStreamReader(stream, Charsets.ISO_8859_1)) {
      final char[] buf = new char[2048];
      final StringBuilder sb = new StringBuilder();
      for (;;) {
        int c = r.read(buf);
        if (c < 0) {
          break;
        }
        sb.append(buf, 0, c);
      }
      return sb.toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Returns whether two {@link LookmlSchema} instances are equal. */
  public static boolean equal(LookmlSchema schema1, LookmlSchema schema2) {
    return new Comparer(difference -> { }).equalSchema(schema1, schema2);
  }

  /** Returns a list of differences between two {@link LookmlSchema}
   * instances. The list is empty if and only if they are equal. */
  public static List<String> compare(LookmlSchema schema1,
      LookmlSchema schema2) {
    final ImmutableList.Builder<String> differences = ImmutableList.builder();
    new Comparer(differences::add).equalSchema(schema1, schema2);
    return differences.build();
  }

  /** Creates a schema for "Schema LookML". It can be used to validate any
   * schema, including itself.
   *
   * <p>It is equivalent to "/lookml/lkml-schema.lkml". */
  public static LookmlSchema schemaSchema() {
    return SCHEMA_SCHEMA_SUPPLIER.get();
  }

  static LookmlSchema schemaSchema_() {
    return schemaBuilder()
        .setName("schema")
        .addEnum("type", "number", "string", "enum", "code", "object",
            "named_object", "ref", "ref_list", "string_list",
            "ref_string_map", "ref_string")
        .addObjectType("enum_type", b ->
            b.addStringListProperty("values")
                .build())
        .addObjectType("object_type", b ->
            b.addNamedObjectProperty("property")
                .build())
        .addObjectType("property", b ->
            b.addRefProperty("type")
                .build())
        .addObjectType("schema", b ->
            b.addNamedObjectProperty("enum_type")
                .addNamedObjectProperty("object_type")
                .addRefListProperty("root_properties")
                .build())
        .addNamedObjectProperty("schema")
        .build();
  }

  /** Validates that a LookML document contains at least one instance of
   * every property in a schema.
   *
   * <p>It is strongly recommended to have an exhaustive 'example document'
   * for each schema. This method can be used to check that document. */
  public static void checkCompleteness(LookmlSchema schema,
      Consumer<ObjectHandler> parseDocument, List<String> errorList) {
    final Set<LookmlSchema.Property> propertiesSeen = new LinkedHashSet<>();
    final PropertyHandler completenessChecker =
        LaxHandlers.completenessChecker(schema, propertiesSeen::add);
    final ErrorHandler errorHandler = LaxHandlers.errorLogger(errorList::add);
    final ObjectHandler validator =
        LaxHandlers.validator(completenessChecker, schema, errorHandler);
    parseDocument.accept(validator);

    // Check that there are some properties, and we saw all of them.
    Set<LookmlSchema.Property> allProperties = new LinkedHashSet<>();
    schema.objectTypes().values().forEach(objectType ->
        allProperties.addAll(objectType.properties().values()));
    Set<LookmlSchema.Property> propertiesNotSeen =
        new LinkedHashSet<>(allProperties);
    propertiesNotSeen.removeAll(propertiesSeen);
    if (allProperties.isEmpty()) {
      errorList.add("schema has no properties");
    }
    if (propertiesSeen.isEmpty()) {
      errorList.add("example document contained no properties");
    }
    if (!propertiesNotSeen.isEmpty()) {
      propertiesNotSeen.forEach(p ->
          errorList.add("property did not occur: " + p));
    }
  }

  /** Builder for a {@link LookmlSchema}. */
  public interface SchemaBuilder {
    /** Builds a {@code LookmlSchema} object from the contents of this
     * builder. */
    LookmlSchema build();

    /** Sets the name of the schema. */
    SchemaBuilder setName(String name);

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

    /** Returns whether {@code typeName} is an enum type. */
    boolean isEnumType(String typeName);

    /** Looks up an enum type; throws if not found. */
    LookmlSchema.EnumType getEnumType(String typeName);

    /** Returns whether {@code typeName} is an object type. */
    boolean isObjectType(String typeName);

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

    /** Creates a property whose value is a list of strings. */
    ObjectTypeBuilder addStringListProperty(String propertyName);

    /** Creates a property whose value is a list of references. */
    ObjectTypeBuilder addRefListProperty(String propertyName);

    /** Creates a property whose value is a list of reference-string pairs. */
    ObjectTypeBuilder addRefStringMapProperty(String propertyName);

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
    @Nullable String name;
    final Map<String, PropertyImpl> rootPropertyMap = new LinkedHashMap<>();
    final Map<String, EnumTypeImpl> enumTypes = new LinkedHashMap<>();
    final Map<String, ObjectTypeImpl> objectTypes = new LinkedHashMap<>();

    @Override public LookmlSchema build() {
      // Deduce the set of 'code' properties
      final Set<String> codePropertyNames = new TreeSet<>();
      forEachProperty(property -> {
        switch (property.type()) {
        case CODE:
          codePropertyNames.add(property.name());
          break;
        case NAMED_OBJECT:
        case OBJECT:
          if (!objectTypes.containsKey(property.name())) {
            throw new IllegalArgumentException("property '" + property.name()
                + "' references unknown object type '" + property.name()
                + "'");
          }
          break;
        case ENUM:
          if (!enumTypes.containsKey(property.typeName())) {
            throw new IllegalArgumentException("property '" + property.name()
                + "' references unknown enum type '" + property.typeName()
                + "'");
          }
          break;
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
      return new SchemaImpl(requireNonNull(name, "name"), rootPropertyMap,
          objectTypes, enumTypes, codePropertyNames);
    }

    private void forEachProperty(Consumer<LookmlSchema.Property> consumer) {
      objectTypes.values().forEach(objectType ->
          objectType.propertyMap.values().forEach(consumer));
    }

    @Override public SchemaBuilder setName(String name) {
      this.name = name;
      return this;
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

    @Override public boolean isEnumType(String typeName) {
      return enumTypes.containsKey(typeName);
    }

    @Override public LookmlSchema.EnumType getEnumType(String typeName) {
      return requireNonNull(enumTypes.get(typeName), typeName);
    }

    @Override public boolean isObjectType(String typeName) {
      return objectTypes.containsKey(typeName);
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
    final String name;
    final Map<String, Property> rootPropertyMap;
    final Map<String, ObjectType> objectTypes;
    final Map<String, EnumType> enumTypes;
    final SortedSet<String> codePropertyNames;

    SchemaImpl(String name,
        Map<String, PropertyImpl> rootPropertyMap,
        Map<String, ObjectTypeImpl> objectTypes,
        Map<String, EnumTypeImpl> enumTypes,
        Iterable<String> codePropertyNames) {
      this.name = name;
      this.rootPropertyMap = ImmutableMap.copyOf(rootPropertyMap);
      this.objectTypes = ImmutableMap.copyOf(objectTypes);
      this.enumTypes = ImmutableMap.copyOf(enumTypes);
      this.codePropertyNames = ImmutableSortedSet.copyOf(codePropertyNames);
    }

    public String name() {
      return name;
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

    @Override public String toString() {
      return propertyMap.values().toString();
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
      this.name = requireNonNull(name);
      this.type = requireNonNull(type);
      this.typeName = requireNonNull(typeName);
      checkArgument(type != LookmlSchema.Type.REF_STRING);
    }

    @Override public String toString() {
      return "property '" + name + "'";
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

    @Override public ObjectTypeBuilder addStringListProperty(
        String propertyName) {
      return addProperty_(propertyName, LookmlSchema.Type.STRING_LIST, "");
    }

    @Override public ObjectTypeBuilder addRefListProperty(String propertyName) {
      return addProperty_(propertyName, LookmlSchema.Type.REF_LIST, "");
    }

    @Override public ObjectTypeBuilder addRefStringMapProperty(
        String propertyName) {
      return addProperty_(propertyName, LookmlSchema.Type.REF_STRING_MAP, "");
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
      case OBJECT:
      case NAMED_OBJECT:
        if (typeName.isEmpty()) {
          throw new IllegalArgumentException(typeName);
        }
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

  /** Walks an AST and calls SchemaBuilder. */
  static class AstWalker {
    final SchemaBuilder b;

    AstWalker(SchemaBuilder b) {
      this.b = b;
    }

    void accept(PairList<String, Value> pairList) {
      pairList.forEach((property, value) -> {
        switch (property) {
        case "schema":
          acceptSchema((Values.NamedObjectValue) value);
          break;
        default:
          throw new AssertionError("unexpected: " + property);
        }
      });
    }

    void acceptSchema(Values.NamedObjectValue objectValue) {
      b.setName(objectValue.name);
      objectValue.properties.forEach((property, value) -> {
        switch (property) {
        case "root_properties":
          acceptRootProperties((Values.ListValue) value);
          break;
        case "enum_type":
          acceptEnumType((Values.NamedObjectValue) value);
          break;
        case "object_type":
          final Values.NamedObjectValue objectValue1 =
              (Values.NamedObjectValue) value;
          b.addObjectType(objectValue1.name, objectTypeBuilder ->
              acceptObjectType(objectTypeBuilder, objectValue1));
          break;
        }
      });
    }

    void acceptRootProperties(Values.ListValue listValue) {
      for (Value value : listValue.list) {
        b.addNamedObjectProperty(((Values.IdentifierValue) value).id);
      }
    }

    void acceptEnumType(Values.NamedObjectValue objectValue) {
      final List<String> valueList = new ArrayList<>();
      objectValue.properties.forEach((property, value) -> {
        switch (property) {
        case "values":
          acceptValues((Values.ListValue) value, valueList);
          break;
        default:
          throw new AssertionError("unexpected: " + property);
        }
      });
      b.addEnum(objectValue.name, valueList);
    }

    void acceptValues(Values.ListValue listValue, List<String> valueList) {
      listValue.list.forEach(value ->
          valueList.add(((Values.StringValue) value).s));
    }

    LookmlSchema.ObjectType acceptObjectType(
        ObjectTypeBuilder objectTypeBuilder,
        Values.NamedObjectValue objectValue) {
      objectValue.properties.forEach((property, value) -> {
        switch (property) {
        case "property":
          acceptProperty(objectTypeBuilder, (Values.NamedObjectValue) value);
          break;
        default:
          throw new AssertionError("unexpected: " + property);
        }
      });
      return objectTypeBuilder.build();
    }

    void acceptProperty(ObjectTypeBuilder objectTypeBuilder,
        Values.NamedObjectValue objectValue) {
      final Values.IdentifierValue value =
          (Values.IdentifierValue) get(objectValue.properties, "type");
      switch (value.id) {
      case "code":
        objectTypeBuilder.addCodeProperty(objectValue.name);
        break;
      case "named_object":
        objectTypeBuilder.addNamedObjectProperty(objectValue.name);
        break;
      case "numeric":
        objectTypeBuilder.addNumberProperty(objectValue.name);
        break;
      case "object":
        objectTypeBuilder.addObjectProperty(objectValue.name);
        break;
      case "ref":
        objectTypeBuilder.addRefProperty(objectValue.name);
        break;
      case "ref_list":
        objectTypeBuilder.addRefListProperty(objectValue.name);
        break;
      case "ref_string_map":
        objectTypeBuilder.addRefStringMapProperty(objectValue.name);
        break;
      case "string":
        objectTypeBuilder.addStringProperty(objectValue.name);
        break;
      case "string_list":
        objectTypeBuilder.addStringListProperty(objectValue.name);
        break;
      default:
        if (b.isEnumType(value.id)) {
          objectTypeBuilder.addEnumProperty(objectValue.name, value.id);
        } else {
          throw new AssertionError("unexpected: " + value.id);
        }
      }
    }

    <K, V> V get(PairList<K, V> pairList, K seek) {
      for (int i = 0; i < pairList.size(); i++) {
        if (pairList.left(i).equals(seek)) {
          return pairList.right(i);
        }
      }
      throw new IllegalArgumentException("not found: " + seek);
    }
  }

  /** Compares whether two instances of
   * {@link net.hydromatic.filtex.lookml.LookmlSchema} are equal,
   * and similarly their component enum types, object types, properties. */
  private static class Comparer {
    private final Consumer<String> differences;

    Comparer(Consumer<String> differences) {
      this.differences = requireNonNull(differences);
    }

    boolean differ(String area, Object left, Object right) {
      differences.accept(area + " (" + left + " vs " + right + ")");
      return false;
    }

    /** Returns whether two {@link LookmlSchema} instances are equal. */
    boolean equalSchema(LookmlSchema s1, LookmlSchema s2) {
      if (!Objects.equals(s1.name(), s2.name())) {
        return differ("schema name", s1.name(), s2.name());
      }
      if (!equalMap("enum type", s1.enumTypes(), s2.enumTypes(),
          this::equalEnumType)) {
        return false;
      }
      if (!equalMap("object type", s1.objectTypes(), s2.objectTypes(),
          this::equalObjectType)) {
        return false;
      }
      if (!equalMap("root properties", s1.rootProperties(), s2.rootProperties(),
          this::equalProperty)) {
        return false;
      }
      return true;
    }

    /** Compares two maps: keys by equality, values using a provided
     * comparer. */
    <K, V> boolean equalMap(String area, Map<K, V> map1, Map<K, V> map2,
        CompareFunction<V> valueComparer) {
      if (!Objects.equals(map1.keySet(), map2.keySet())) {
        return differ(area + " names", map1.keySet(), map2.keySet());
      }
      for (Map.Entry<K, V> entry : map1.entrySet()) {
        final V value1 = entry.getValue();
        final V value2 = map2.get(entry.getKey());
        final String area2 = area + ", value '" + entry.getKey() + "'";
        if (value2 == null) {
          return differ(area2, value1, "(missing)");
        }
        if (!valueComparer.compare(area2, value1, value2)) {
          return differ(area2, value1, value2);
        }
      }
      return true;
    }

    boolean equalEnumType(String area, LookmlSchema.EnumType type1,
        LookmlSchema.EnumType type2) {
      if (!Objects.equals(type1.allowedValues(), type2.allowedValues())) {
        return differ(area, type1, type2);
      }
      return true;
    }

    boolean equalObjectType(String area, LookmlSchema.ObjectType type1,
        LookmlSchema.ObjectType type2) {
      return equalMap(area, type1.properties(), type2.properties(),
          this::equalProperty);
    }

    boolean equalProperty(String area, LookmlSchema.Property property1,
        LookmlSchema.Property property2) {
      if (!Objects.equals(property1.name(), property2.name())) {
        return differ(area + " name", property1.name(), property2.name());
      }
      if (!Objects.equals(property1.type(), property2.type())) {
        return differ(area + " type", property1.type(), property2.type());
      }
      if (!Objects.equals(property1.typeName(), property2.typeName())) {
        return differ(area + " type name", property1.typeName(),
            property2.typeName());
      }
      return true;
    }
  }

  /** Function that compares values from left and right and reports
   * the location of the difference if they are not the same.
   *
   * @param <E> Type of values to be compared.
   */
  interface CompareFunction<E> {
    boolean compare(String area, E left, E right);
  }
}

// End LookmlSchemas.java
