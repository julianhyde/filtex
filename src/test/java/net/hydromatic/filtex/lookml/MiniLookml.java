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

import net.hydromatic.filtex.LaxTest;
import net.hydromatic.filtex.util.ImmutablePairList;
import net.hydromatic.filtex.util.PairList;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Defines a very small subset of LookML, called Mini-LookML.
 *
 * <p>Mini-LookML is used for testing and also as an example for creating
 * more serious dialects (schemas) of LookML.
 *
 * <p>Mini-LookML has at least one example of each syntactic type of property
 * (number, string, boolean, ref-list, etc.) We also provide an example model,
 * {@link #exampleModel}, that contains at least one instance of each property.
 * It can therefore be used to validate builders, writers, and so forth.
 *
 * <p>Also contains an object model, consisting of classes such as
 * {@link Model}, {@link Explore}, {@link View}, {@link Dimension},
 * {@link Measure}, created via the {@link #builder(LookmlSchema, BiConsumer)}
 * method. The object model is written by hand, but tests verify that it is
 * synchronized with the schema. */
public class MiniLookml {
  private MiniLookml() {}

  /** Caches a LookML string that contains at least one instance
   * of every property in {@link #schema()}. */
  private static final Supplier<String> EXAMPLE_MODEL_SUPPLIER =
      Suppliers.memoize(() ->
          LookmlSchemas.urlContents(
              LaxTest.class.getResource(
                  "/lookml/mini-lookml-example-model.lkml")));

  /** Caches the schema. */
  private static final Supplier<LookmlSchema> SCHEMA_SUPPLIER =
      Suppliers.memoize(MiniLookml::schema_);

  /** Caches the template, which provides a factory method for each object
   * type. */
  private static final Supplier<Template> TEMPLATE_SUPPLIER =
      Suppliers.memoize(() ->
          Template.create(schema(),
              ImmutableMap.<String, Function<String, NodeBuilder>>builder()
                  .put("model", ModelBuilder::new)
                  .put("explore", ExploreBuilder::new)
                  .put("view", ViewBuilder::new)
                  .put("join", JoinBuilder::new)
                  .put("measure", MeasureBuilder::new)
                  .put("dimension", DimensionBuilder::new)
                  .put("conditionally_filter",
                      name -> new ConditionallyFilterBuilder())
                  .build(),
              ImmutableMap.<String, Class<? extends Enum<?>>>builder()
                  .put("boolean", YesNo.class)
                  .put("join_type", JoinType.class)
                  .put("relationship_type", RelationshipType.class)
                  .put("dimension_field_type", DimensionType.class)
                  .put("measure_field_type", MeasureType.class)
                  .build()));

  /** Returns the LookML source text of an example model. */
  public static String exampleModel() {
    return EXAMPLE_MODEL_SUPPLIER.get();
  }

  /** Returns the schema of Mini-LookML. */
  public static LookmlSchema schema() {
    return SCHEMA_SUPPLIER.get();
  }

  private static LookmlSchema schema_() {
    return LookmlSchemas.schemaBuilder()
        .setName("core")
        .addEnum("boolean", "false", "true")
        .addEnum("join_type", "left_outer", "full_outer", "inner", "cross")
        .addEnum("relationship_type", "many_to_one", "many_to_many",
            "one_to_many", "one_to_one")
        .addEnum("dimension_field_type", "bin", "date", "date_time", "distance",
            "duration", "location", "number", "string", "tier", "time",
            "unquoted", "yesno", "zipcode")
        .addEnum("measure_field_type", "average", "average_distinct", "count",
            "count_distinct", "date", "list", "max", "median",
            "median_distinct", "min", "number", "percent_of_previous",
            "percent_of_total", "percentile", "percentile_distinct",
            "running_total", "string", "sum", "sum_distinct", "yesno")
        .addObjectType("conditionally_filter", b ->
            b.addRefStringMapProperty("filters")
                .addRefListProperty("unless")
                .build())
        .addObjectType("dimension", b ->
            b.addEnumProperty("type", "dimension_field_type")
                .addCodeProperty("sql")
                .addStringProperty("label")
                .addEnumProperty("primary_key", "boolean")
                .addStringListProperty("tags")
                .build())
        .addObjectType("measure", b ->
            b.addEnumProperty("type", "measure_field_type")
                .addCodeProperty("sql")
                .addStringProperty("label")
                .build())
        .addObjectType("view", b ->
            b.addRefProperty("from")
                .addStringProperty("label")
                .addCodeProperty("sql_table_name")
                .addNamedObjectProperty("dimension")
                .addNamedObjectProperty("measure")
                .addRefListProperty("drill_fields")
                .build())
        .addObjectType("join", b ->
            b.addRefProperty("from")
                .addCodeProperty("sql_on")
                .addEnumProperty("relationship", "relationship_type")
                .build())
        .addObjectType("explore", b ->
            b.addRefProperty("from")
                .addRefProperty("view_name")
                .addNamedObjectProperty("join")
                .addObjectProperty("conditionally_filter")
                .build())
        .addNamedObjectProperty("model", b ->
            b.addNamedObjectProperty("explore")
                .addNamedObjectProperty("view")
                .addNumberProperty("fiscal_month_offset")
                .build())
        .build();
  }

  /** Returns the URL of a file that contains the Mini-LookML schema.
   *
   * <p>The contents of this file creates a schema identical to that returned
   * from {@link #schema()}. This is verified by a test. */
  public static URL getSchemaUrl() {
    return LaxTest.class.getResource("/lookml/mini-lookml-schema.lkml");
  }

  /** Creates a builder for Mini-LookML's AST.
   *
   * <p>The builder implements the {@link PropertyHandler} interface so that it
   * can receive a stream of document events. */
  public static PropertyHandler builder(LookmlSchema ignore,
      BiConsumer<String, Object> consumer) {
    return new RootBuilder(consumer, TEMPLATE_SUPPLIER.get());
  }

  /** Validates a Mini-LookML AST. */
  public static class Validator {
    public void validate(Model model,
        List<String> errorList) {
    }
  }

  /** Template contains factories for building ASTs for a particular schema.
   *
   * <p>For each object type there is a factory, and for each enum type there
   * is a Java enum class. */
  private static class Template {
    final LookmlSchema schema;
    final Map<String, Function<String, NodeBuilder>> typeFactories;
    final Map<String, Class<? extends Enum<?>>> enumClasses;

    private Template(LookmlSchema schema,
        Map<String, Function<String, NodeBuilder>> typeFactories,
        Map<String, Class<? extends Enum<?>>> enumClasses) {
      this.schema = schema;
      this.typeFactories = ImmutableMap.copyOf(typeFactories);
      this.enumClasses = ImmutableMap.copyOf(enumClasses);
    }

    static Template create(LookmlSchema schema,
        Map<String, Function<String, NodeBuilder>> typeFactories,
        Map<String, Class<? extends Enum<?>>> enumClasses) {
      schema.objectTypes().forEach((name, type) -> {
        final Function<String, NodeBuilder> factory =
            typeFactories.get(name);
        if (factory == null) {
          throw new IllegalArgumentException("no factory for object type "
              + name);
        }
      });
      schema.enumTypes().forEach((name, type) -> {
        Class<? extends Enum<?>> enumClass = enumClasses.get(name);
        if (enumClass == null) {
          throw new IllegalArgumentException("no class for enum type "
              + name);
        }
        final SortedSet<String> enumConstantNames = new TreeSet<>();
        type.allowedValues().forEach(allowedValue ->
            enumConstantNames.add(allowedValue.toUpperCase(Locale.ROOT)));
        for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
          enumConstantNames.remove(enumConstant.name());
        }
        if (!enumConstantNames.isEmpty()) {
          throw new IllegalArgumentException("enum class " + enumClass
              + " (for enum type " + name
              + ") is missing the following constants: " + enumConstantNames);
        }
      });
      return new Template(schema, typeFactories, enumClasses);
    }
  }

  /** Accepts events indicating completed objects and converts them into a
   * tree of AST nodes. */
  private abstract static class Builder implements PropertyHandler {
    protected PropertyHandler objOpen_(RootBuilder root,
        PropertyHandler parentPropertyHandler,
        LookmlSchema.Property property, String name) {
      final Function<String, NodeBuilder> factory =
          root.template.typeFactories.get(property.name());
      if (factory == null) {
        throw new IllegalArgumentException("unknown type " + property.name());
      }
      final NodeBuilder subNodeBuilder = factory.apply(name);
      return new NonRootBuilder(root, parentPropertyHandler, property,
          subNodeBuilder);
    }
  }

  /** Extension to {@link Builder} that is not the root; it belongs to a parent
   * via a property, and also has a reference to the root. */
  private static class NonRootBuilder extends Builder {
    final RootBuilder root;
    final PropertyHandler parentPropertyHandler;
    private final LookmlSchema.Property property;
    final NodeBuilder nodeBuilder;

    NonRootBuilder(RootBuilder root, PropertyHandler parentPropertyHandler,
        LookmlSchema.Property property, NodeBuilder nodeBuilder) {
      this.root = root;
      this.parentPropertyHandler = parentPropertyHandler;
      this.property = property;
      this.nodeBuilder = nodeBuilder;
    }

    @Override public PropertyHandler property(LookmlSchema.Property property,
        Object value) {
      nodeBuilder.accept(property.name(), value);
      return this;
    }

    @Override public ListHandler listOpen(LookmlSchema.Property property) {
      return new LaxHandlers.ListBuilder(list ->
          nodeBuilder.accept(property.name(), list));
    }

    @Override public PropertyHandler objOpen(LookmlSchema.Property property) {
      return objOpen_(root, this, property, "");
    }

    @Override public PropertyHandler objOpen(LookmlSchema.Property property,
        String name) {
      return objOpen_(root, this, property, name);
    }

    @Override public void close() {
      parentPropertyHandler.property(property, nodeBuilder.build());
    }
  }

  /** Extension to {@link Builder} that contains state shared throughout the
   * tree. */
  private static class RootBuilder extends Builder {
    private final Template template;
    private final BiConsumer<String, Object> consumer;

    private RootBuilder(BiConsumer<String, Object> consumer,
        Template template) {
      this.consumer = consumer;
      this.template = template;
    }

    @Override public PropertyHandler property(LookmlSchema.Property property,
        Object value) {
      consumer.accept(property.name(),  value);
      return this;
    }

    @Override public ListHandler listOpen(LookmlSchema.Property property) {
      throw new UnsupportedOperationException();
    }

    @Override public PropertyHandler objOpen(LookmlSchema.Property property) {
      return objOpen_(this, this, property, "");
    }

    @Override public PropertyHandler objOpen(LookmlSchema.Property property,
        String name) {
      return objOpen_(this, this, property, name);
    }

    @Override public void close() {
    }
  }

  /** Can build a node in the AST. */
  private abstract static class NodeBuilder {
    abstract void accept(String name, Object value);
    abstract Object build();
  }

  /** Builds a {@link Model}. */
  private static class ModelBuilder extends NodeBuilder {
    private final String name;
    private int fiscalMonthOffset = 0;
    private final ImmutableMap.Builder<String, Explore> explores =
        ImmutableMap.builder();
    private final ImmutableMap.Builder<String, View> views =
        ImmutableMap.builder();

    ModelBuilder(String name) {
      this.name = name;
    }

    void accept(String key, Object value) {
      switch (key) {
      case "explore":
        Explore explore = (Explore) value;
        explores.put(explore.name, explore);
        break;
      case "view":
        View view = (View) value;
        views.put(view.name, view);
        break;
      case "fiscal_month_offset":
        fiscalMonthOffset = ((Number) value).intValue();
        break;
      default:
        throw new IllegalArgumentException("unknown property " + key);
      }
    }

    Model build() {
      return new Model(name, fiscalMonthOffset, explores.build(),
          views.build());
    }
  }

  /** Instance of Mini-LookML's "model" type. */
  public static class Model {
    public final String name;
    public final int fiscalMonthOffset;
    public final Map<String, Explore> explores;
    public final Map<String, View> views;

    Model(String name,
        int fiscalMonthOffset,
        ImmutableMap<String, Explore> explores,
        ImmutableMap<String, View> views) {
      this.name = name;
      this.fiscalMonthOffset = fiscalMonthOffset;
      this.explores = explores;
      this.views = views;
    }
  }

  /** Builds a {@link View}. */
  private static class ViewBuilder extends NodeBuilder {
    private final String name;
    private String from;
    private String label;
    private String sqlTableName;
    private final ImmutableMap.Builder<String, Dimension> dimensions =
        ImmutableMap.builder();
    private final ImmutableMap.Builder<String, Measure> measures =
        ImmutableMap.builder();
    private final ImmutableList.Builder<String> drillFields =
        ImmutableList.builder();

    ViewBuilder(String name) {
      this.name = name;
    }

    void accept(String key, Object value) {
      switch (key) {
      case "from":
        from = (String) value;
        break;
      case "label":
        label = (String) value;
        break;
      case "sql_table_name":
        sqlTableName = (String) value;
        break;
      case "dimension":
        Dimension dimension = (Dimension) value;
        dimensions.put(dimension.name, dimension);
        break;
      case "measure":
        Measure measure = (Measure) value;
        measures.put(measure.name, measure);
        break;
      case "drill_fields":
        ((List<Values.StringValue>) value).forEach(s -> drillFields.add(s.s));
        break;
      default:
        throw new IllegalArgumentException("unknown property " + key);
      }
    }

    View build() {
      return new View(name, from, label, sqlTableName, dimensions.build(),
          measures.build());
    }
  }

  /** Instance of Mini-LookML's "view" type. */
  public static class View {
    public final String name;
    public final String from;
    public final String label;
    public final String sqlTableName;
    public final Map<String, Dimension> dimensions;
    public final Map<String, Measure> measures;

    View(String name, String from, String label, String sqlTableName,
        Map<String, Dimension> dimensions, Map<String, Measure> measures) {
      this.name = name;
      this.from = from;
      this.label = label;
      this.sqlTableName = sqlTableName;
      this.dimensions = dimensions;
      this.measures = measures;
    }
  }

  /** Builds an {@link Explore}. */
  private static class ExploreBuilder extends NodeBuilder {
    private final String name;
    private String from;
    private String viewName;
    private Join join;
    private ConditionallyFilter conditionallyFilter;

    ExploreBuilder(String name) {
      this.name = name;
    }

    void accept(String key, Object value) {
      switch (key) {
      case "from":
        from = (String) value;
        break;
      case "view_name":
        viewName = (String) value;
        break;
      case "join":
        join = (Join) value;
        break;
      case "conditionally_filter":
        conditionallyFilter = (ConditionallyFilter) value;
        break;
      default:
        throw new IllegalArgumentException("unknown property " + key);
      }
    }

    Explore build() {
      return new Explore(name, from, viewName, join, conditionallyFilter);
    }
  }

  /** Instance of Mini-LookML's "explore" type. */
  public static class Explore {
    public final String name;
    public final String from;
    public final String viewName;
    public final Join join;
    public final ConditionallyFilter conditionallyFilter;

    Explore(String name, String from, String viewName, Join join,
        ConditionallyFilter conditionallyFilter) {
      this.name = name;
      this.from = from;
      this.viewName = viewName;
      this.join = join;
      this.conditionallyFilter = conditionallyFilter;
    }
  }

  /** Builds a {@link Join}. */
  private static class JoinBuilder extends NodeBuilder {
    private final String name;
    private String from;
    private String sqlOn;
    private RelationshipType relationship;

    JoinBuilder(String name) {
      this.name = name;
    }

    void accept(String key, Object value) {
      switch (key) {
      case "from":
        from = (String) value;
        break;
      case "sql_on":
        sqlOn = (String) value;
        break;
      case "relationship":
        relationship =
            RelationshipType.valueOf(
                ((String) value).toUpperCase(Locale.ROOT));
        break;
      default:
        throw new IllegalArgumentException("unknown property " + key);
      }
    }

    Join build() {
      return new Join(name, from, sqlOn, relationship);
    }
  }

  /** Instance of Mini-LookML's "join" type. */
  public static class Join {
    public final String name;
    public final String from;
    public final String sqlOn;
    public final RelationshipType relationship;

    public Join(String name, String from, String sqlOn,
        RelationshipType relationship) {
      this.name = name;
      this.from = from;
      this.sqlOn = sqlOn;
      this.relationship = relationship;
    }
  }

  /** Builds a {@link Dimension}. */
  private static class DimensionBuilder extends NodeBuilder {
    private final String name;
    private DimensionType type;
    private String from;
    private String label;
    private String sqlTableName;
    private final ImmutableList.Builder<String> drillFields =
        ImmutableList.builder();
    private boolean primaryKey;
    private String sql;
    private final ImmutableList.Builder<String> tags = ImmutableList.builder();

    DimensionBuilder(String name) {
      this.name = name;
    }

    void accept(String key, Object value) {
      switch (key) {
      case "type":
        type =
            DimensionType.valueOf(
                ((String) value).toUpperCase(Locale.ROOT));
        break;
      case "from":
        from = (String) value;
        break;
      case "label":
        label = (String) value;
        break;
      case "sql_table_name":
        sqlTableName = (String) value;
        break;
      case "drill_fields":
        ((List<Values.StringValue>) value).forEach(s -> drillFields.add(s.s));
        break;
      case "primary_key":
        primaryKey = Boolean.valueOf((String) value);
        break;
      case "sql":
        sql = (String) value;
        break;
      case "tags":
        ((List<Values.StringValue>) value).forEach(v -> tags.add(v.s));
        break;
      default:
        throw new IllegalArgumentException("unknown property " + key);
      }
    }

    Dimension build() {
      return new Dimension(name, type, from, label, sqlTableName, primaryKey,
          sql, tags.build());
    }
  }

  /** Instance of Mini-LookML's "dimension" type. */
  public static class Dimension {
    public final String name;
    public final DimensionType type;
    public final String from;
    public final String label;
    public final String sqlTableName;
    public final boolean primaryKey;
    public final String sql;
    public final List<String> tags;

    public Dimension(String name, DimensionType type, String from, String label,
        String sqlTableName, boolean primaryKey, String sql,
        ImmutableList<String> tags) {
      this.name = name;
      this.type = type;
      this.from = from;
      this.label = label;
      this.sqlTableName = sqlTableName;
      this.primaryKey = primaryKey;
      this.sql = sql;
      this.tags = tags;
    }
  }

  /** Builds a {@link Measure}. */
  private static class MeasureBuilder extends NodeBuilder {
    private final String name;
    private String label;
    private final ImmutableList.Builder<String> drillFields =
        ImmutableList.builder();
    private MeasureType type;
    private String sql;

    MeasureBuilder(String name) {
      this.name = name;
    }

    void accept(String key, Object value) {
      switch (key) {
      case "type":
        type =
            MeasureType.valueOf(
                ((String) value).toUpperCase(Locale.ROOT));
        break;
      case "label":
        label = (String) value;
        break;
      case "drill_fields":
        ((List<Values.StringValue>) value).forEach(s -> drillFields.add(s.s));
        break;
      case "sql":
        sql = (String) value;
        break;
      default:
        throw new IllegalArgumentException("unknown property " + key);
      }
    }

    Measure build() {
      return new Measure(name, label, type, sql);
    }
  }

  /** Instance of Mini-LookML's "measure" type. */
  public static class Measure {
    public final String name;
    public final String label;
    public final MeasureType type;
    public final String sql;

    public Measure(String name, String label, MeasureType type, String sql) {
      this.name = name;
      this.label = label;
      this.type = type;
      this.sql = sql;
    }
  }

  /** Builds a {@link ConditionallyFilter}. */
  private static class ConditionallyFilterBuilder extends NodeBuilder {
    private final PairList<String, String> filters = PairList.of();
    private final ImmutableList.Builder<String> unless =
        ImmutableList.builder();

    void accept(String key, Object value) {
      switch (key) {
      case "filters":
        ((List<Values.PairValue>) value)
            .forEach(pair -> filters.add(pair.ref, pair.s));
        break;
      case "unless":
        ((List<Values.IdentifierValue>) value).forEach(s -> unless.add(s.id));
        break;
      default:
        throw new IllegalArgumentException("unknown property " + key);
      }
    }

    ConditionallyFilter build() {
      return new ConditionallyFilter(filters.immutable(), unless.build());
    }
  }

  /** Instance of Mini-LookML's "conditionally_filter" type. */
  public static class ConditionallyFilter {
    public final PairList<String, String> filters;
    public final List<String> unless;

    public ConditionallyFilter(ImmutablePairList<String, String> filters,
        ImmutableList<String> unless) {
      this.filters = filters;
      this.unless = unless;
    }
  }

  /** Enum corresponding to Mini-LookML's "relationship_type" enum type. */
  public enum RelationshipType {
    MANY_TO_MANY, MANY_TO_ONE, ONE_TO_MANY, ONE_TO_ONE
  }

  /** Enum corresponding to Mini-LookML's "dimension_field_type" enum type. */
  public enum DimensionType {
    BIN, DATE, DATE_TIME, DISTANCE, DURATION, LOCATION, NUMBER, STRING, TIER,
    TIME, UNQUOTED, YESNO, ZIPCODE
  }

  /** Enum corresponding to Mini-LookML's "measure_field_type" enum type. */
  public enum MeasureType {
    AVERAGE, AVERAGE_DISTINCT, COUNT, COUNT_DISTINCT, DATE, LIST, MAX,
    MEDIAN, MEDIAN_DISTINCT, MIN, NUMBER, PERCENTILE, PERCENTILE_DISTINCT,
    PERCENT_OF_PREVIOUS, PERCENT_OF_TOTAL, RUNNING_TOTAL, STRING,
    SUM, SUM_DISTINCT, YESNO
  }

  /** Enum corresponding to Mini-LookML's "boolean" enum type. */
  public enum YesNo {
    FALSE, TRUE
  }

  /** Enum corresponding to Mini-LookML's "join_type" enum type. */
  public enum JoinType {
    CROSS, FULL_OUTER, INNER, LEFT_OUTER
  }
}

// End MiniLookml.java
