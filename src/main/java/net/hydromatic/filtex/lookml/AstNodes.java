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

import com.google.common.collect.ImmutableMap;

import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/** One class for each type in a LookML-Lite model. */
public class AstNodes {
  private AstNodes() {}

  public static PropertyHandler builder(LookmlSchema schema,
      BiConsumer<String, Object> consumer) {
    return new RootBuilder(schema, consumer);
  }

  /** Accepts events indicating completed objects and converts them into a
   * tree of AST nodes. */
  public abstract static class Builder implements PropertyHandler {
    protected PropertyHandler objOpen_(RootBuilder root,
        PropertyHandler parentPropertyHandler,
        LookmlSchema.Property property, String name) {
      final Function<String, NodeBuilder> factory =
          root.typeBuilders.get(property.name());
      if (factory == null) {
        throw new IllegalArgumentException("unknown type " + property.name());
      }
      final NodeBuilder subNodeBuilder = factory.apply(name);
      return new NonRootBuilder(root, parentPropertyHandler, property,
          subNodeBuilder);
    }
  }

  static class NonRootBuilder extends Builder {
    private final RootBuilder root;
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

  protected static class RootBuilder extends Builder {
    private final Map<String, Function<String, NodeBuilder>> typeBuilders;
    private final LookmlSchema schema;
    private final BiConsumer<String, Object> consumer;
    private final Map<String, RelationshipType> relationshipTypeMap;

    private RootBuilder(LookmlSchema schema,
        BiConsumer<String, Object> consumer) {
      this.schema = schema;
      this.consumer = consumer;
      this.typeBuilders =
          ImmutableMap.<String, Function<String, NodeBuilder>>builder()
              .put("model", ModelBuilder::new)
              .put("explore", ExploreBuilder::new)
              .put("view", ViewBuilder::new)
              .put("join", JoinBuilder::new)
              .put("measure", MeasureBuilder::new)
              .put("dimension", DimensionBuilder::new)
              .build();
      this.relationshipTypeMap =
          enumConverter(RelationshipType.class, "relationship_type");
    }

    private <E extends Enum<E>> Map<String, E> enumConverter(Class<E> enumClass,
        String enumTypeName) {
      LookmlSchema.EnumType enumType = schema.enumTypes().get(enumTypeName);
      requireNonNull(enumType);
      ImmutableMap.Builder<String, E> map = ImmutableMap.builder();
      enumType.allowedValues().forEach(value ->
          map.put(value,
              Enum.valueOf(enumClass, value.toUpperCase(Locale.ROOT))));
      return map.build();
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
  abstract static class NodeBuilder {
    abstract void accept(String name, Object value);
    abstract Object build();
  }

  /** Builds a {@link Model}. */
  static class ModelBuilder extends NodeBuilder {
    private final String name;
    int fiscalMonthOffset = 0;
    ImmutableMap.Builder<String, Explore> explores = ImmutableMap.builder();
    ImmutableMap.Builder<String, View> views = ImmutableMap.builder();

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
  static class ViewBuilder extends NodeBuilder {
    final String name;
    String from;
    String label;
    String sqlTableName;
    ImmutableMap.Builder<String, Dimension> dimensions = ImmutableMap.builder();
    ImmutableMap.Builder<String, Measure> measures = ImmutableMap.builder();

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
      default:
        throw new IllegalArgumentException("unknown property " + key);
      }
    }

    View build() {
      return new View(name, label, sqlTableName, dimensions.build(),
          measures.build());
    }
  }

  public static class View {
    public final String name;
    public final String label;
    public final String sqlTableName;
    public final Map<String, Dimension> dimensions;
    public final Map<String, Measure> measures;

    View(String name, String label, String sqlTableName,
        Map<String, Dimension> dimensions, Map<String, Measure> measures) {
      this.name = name;
      this.label = label;
      this.sqlTableName = sqlTableName;
      this.dimensions = dimensions;
      this.measures = measures;
    }
  }

  /** Builds an {@link Explore}. */
  static class ExploreBuilder extends NodeBuilder {
    final String name;
    String from;
    String viewName;
    Join join;
    ImmutableMap.Builder<String, String> conditionallyFilter =
        ImmutableMap.builder();

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
        PairList<String, String> pairList = (PairList<String, String>) value;
        pairList.forEach((ref, s) -> conditionallyFilter.put(ref, s));
        break;
      default:
        throw new IllegalArgumentException("unknown property " + key);
      }
    }

    Explore build() {
      return new Explore(name, from, viewName, join,
          conditionallyFilter.build());
    }
  }

  public static class Explore {
    final String name;
    final String from;
    final String viewName;
    final Join join;
    final ImmutableMap<String, String> conditionallyFilter;

    Explore(String name, String from, String viewName, Join join,
        ImmutableMap<String, String> conditionallyFilter) {
      this.name = name;
      this.from = from;
      this.viewName = viewName;
      this.join = join;
      this.conditionallyFilter = conditionallyFilter;
    }
  }

  /** Builds a {@link Join}. */
  static class JoinBuilder extends NodeBuilder {
    private final String name;
    String from;
    String sqlOn;
    RelationshipType relationship;

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
        relationship = RelationshipType.valueOf((String) value);
        break;
      default:
        throw new IllegalArgumentException("unknown property " + key);
      }
    }

    Join build() {
      return new Join(name);
    }
  }

  public static class Join {
    public final String name;

    public Join(String name) {
      this.name = name;
    }
  }
  /** Builds a {@link Dimension}. */
  static class DimensionBuilder extends NodeBuilder {
    private final String name;
    String from;
    String label;
    String sqlTableName;
    ImmutableMap.Builder<String, Dimension> dimensions = ImmutableMap.builder();
    ImmutableMap.Builder<String, Measure> measures = ImmutableMap.builder();

    DimensionBuilder(String name) {
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
      default:
        throw new IllegalArgumentException("unknown property " + key);
      }
    }

    Dimension build() {
      return new Dimension(name);
    }
  }

  public static class Dimension {
    public final String name;

    public Dimension(String name) {
      this.name = name;
    }
  }
  /** Builds a {@link Measure}. */
  static class MeasureBuilder extends NodeBuilder {
    private final String name;

    MeasureBuilder(String name) {
      this.name = name;
    }

    void accept(String key, Object value) {
      switch (key) {
      default:
        throw new IllegalArgumentException("unknown property " + key);
      }
    }

    Measure build() {
      return new Measure(name);
    }
  }

  public static class Measure {
    public final String name;

    public Measure(String name) {
      this.name = name;
    }
  }

  enum RelationshipType {
    MANY_TO_MANY,
    MANY_TO_ONE,
    ONE_TO_ONE,
    ONE_TO_MANY
  }
}

// End AstNodes.java
