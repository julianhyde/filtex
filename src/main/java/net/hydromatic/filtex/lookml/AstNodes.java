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

import net.hydromatic.filtex.util.PairList;

import java.util.Map;
import java.util.function.Function;

/** One class for each type in a LookML-Lite model. */
public class AstNodes {
  public static Builder builder(LookmlSchema schema) {
    return new Builder(schema);
  }

  public static class Builder implements ScopedObjectHandler.PolyBuilder {
    private final LookmlSchema schema;
    private final Map<String, Function<String, NodeBuilder<?>>> typeBuilders;

    private Builder(LookmlSchema schema) {
      this.schema = schema;
      this.typeBuilders =
          ImmutableMap.<String, Function<String, NodeBuilder<?>>>builder()
              .put("model", ModelBuilder::new)
              .put("explore", ExploreBuilder::new)
              .put("view", ViewBuilder::new)
              .put("join", JoinBuilder::new)
              .put("measure", MeasureBuilder::new)
              .put("dimension", DimensionBuilder::new)
              .build();
    }

    @Override public Object build(String typeName,
        LookmlSchema.ObjectType objectType,
        String name, PairList<String, Value> properties) {
      final Function<String, NodeBuilder<?>> function =
          typeBuilders.get(typeName);
      if (function == null) {
        throw new IllegalArgumentException("unknown type " + typeName);
      }
      NodeBuilder<?> b = function.apply(name);
      properties.forEach(b::accept);
      return b.build();
    }
  }

  /** Can build a node in the AST. */
  static abstract class NodeBuilder<T> {
    abstract void accept(String name, Value value);
    abstract T build();
  }

  /** Builds a {@link Model}. */
  static class ModelBuilder extends NodeBuilder<Model> {
    private final String name;
    int fiscalMonthOffset = 0;
    ImmutableMap.Builder<String, Explore> explores = ImmutableMap.builder();
    ImmutableMap.Builder<String, View> views = ImmutableMap.builder();

    ModelBuilder(String name) {
      this.name = name;
    }

    void accept(String key, Value value) {
      switch (key) {
      case "explore":
        Explore explore =
            ((Values.WrappedValue) value).unwrap(Explore.class);
        explores.put(explore.name, explore);
        break;
      case "view":
        View view = ((Values.WrappedValue) value).unwrap(View.class);
        views.put(view.name, view);
        break;
      case "fiscal_month_offset":
        fiscalMonthOffset = ((Values.NumberValue) value).number.intValue();
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
  static class ViewBuilder extends NodeBuilder<View> {
    private final String name;
    String from;
    String label;
    String sqlTableName;
    ImmutableMap.Builder<String, Dimension> dimensions = ImmutableMap.builder();
    ImmutableMap.Builder<String, Measure> measures = ImmutableMap.builder();

    ViewBuilder(String name) {
      this.name = name;
    }

    void accept(String key, Value value) {
      switch (key) {
      case "from":
        from = ((Values.StringValue) value).s;
        break;
      case "label":
        label = ((Values.StringValue) value).s;
        break;
      case "sql_table_name":
        sqlTableName = ((Values.StringValue) value).s;
        break;
      case "dimension":
        Dimension dimension =
            ((Values.WrappedValue) value).unwrap(Dimension.class);
        dimensions.put(dimension.name, dimension);
        break;
      case "measure":
        Measure measure =
            ((Values.WrappedValue) value).unwrap(Measure.class);
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
  static class ExploreBuilder extends NodeBuilder<Explore> {
    final String name;
    String from;
    String viewName;
    Join join;
    ImmutableMap.Builder<String, String> conditionallyFilter =
        ImmutableMap.builder();

    ExploreBuilder(String name) {
      this.name = name;
    }

    void accept(String key, Value value) {
      switch (key) {
      case "from":
        from = ((Values.StringValue) value).s;
        break;
      case "view_name":
        viewName = ((Values.IdentifierValue) value).id;
        break;
      case "join":
        join = ((Values.WrappedValue) value).unwrap(Join.class);
        break;
      case "conditionally_filter":
        ((Values.ListValue) value).list.forEach(value2 -> {
          final Values.PairValue pair = (Values.PairValue) value2;
          conditionallyFilter.put(pair.ref, pair.s);
        });
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
  static class JoinBuilder extends NodeBuilder<Join> {
    private final String name;
    String from;
    String sqlOn;
    String sqlTableName;
    ImmutableMap.Builder<String, Dimension> dimensions = ImmutableMap.builder();
    ImmutableMap.Builder<String, Measure> measures = ImmutableMap.builder();

    JoinBuilder(String name) {
      this.name = name;
    }

    void accept(String key, Value value) {
      b.addRefProperty("from")
          .addCodeProperty("sql_on")
          .addEnumProperty("relationship", "relationship_type")
      switch (key) {
      case "from":
        from = ((Values.IdentifierValue) value).id;
        break;
      case "sql_on":
        sqlOn = ((Values.StringValue) value).s;
        break;
      case "sql_table_name":
        sqlTableName = ((Values.StringValue) value).s;
        break;
      case "relationship":
        RelationshipType relationshipType =
            ((Values.IdentifierValue) value).asEnum(RelationshipType.class);
        dimensions.put(dimension.name, dimension);
        break;
      case "measure":
        Measure measure =
            ((Values.WrappedValue) value).unwrap(Measure.class);
        measures.put(measure.name, measure);
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
  static class DimensionBuilder extends NodeBuilder<Dimension> {
    private final String name;
    String from;
    String label;
    String sqlTableName;
    ImmutableMap.Builder<String, Dimension> dimensions = ImmutableMap.builder();
    ImmutableMap.Builder<String, Measure> measures = ImmutableMap.builder();

    DimensionBuilder(String name) {
      this.name = name;
    }

    void accept(String key, Value value) {
      switch (key) {
      case "from":
        from = ((Values.StringValue) value).s;
        break;
      case "label":
        label = ((Values.StringValue) value).s;
        break;
      case "sql_table_name":
        sqlTableName = ((Values.StringValue) value).s;
        break;
      case "dimension":
        Dimension dimension =
            ((Values.WrappedValue) value).unwrap(Dimension.class);
        dimensions.put(dimension.name, dimension);
        break;
      case "measure":
        Measure measure =
            ((Values.WrappedValue) value).unwrap(Measure.class);
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
  static class MeasureBuilder extends NodeBuilder<Measure> {
    private final String name;
    String from;
    String label;
    String sqlTableName;
    ImmutableMap.Builder<String, Dimension> dimensions = ImmutableMap.builder();
    ImmutableMap.Builder<String, Measure> measures = ImmutableMap.builder();

    MeasureBuilder(String name) {
      this.name = name;
    }

    void accept(String key, Value value) {
      switch (key) {
      case "from":
        from = ((Values.StringValue) value).s;
        break;
      case "label":
        label = ((Values.StringValue) value).s;
        break;
      case "sql_table_name":
        sqlTableName = ((Values.StringValue) value).s;
        break;
      case "dimension":
        Dimension dimension =
            ((Values.WrappedValue) value).unwrap(Dimension.class);
        dimensions.put(dimension.name, dimension);
        break;
      case "measure":
        Measure measure =
            ((Values.WrappedValue) value).unwrap(Measure.class);
        measures.put(measure.name, measure);
        break;
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

  }
}

// End AstNodes.java
