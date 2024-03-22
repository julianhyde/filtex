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
package net.hydromatic.filtex;

import net.hydromatic.filtex.lookml.LaxHandlers;
import net.hydromatic.filtex.lookml.LookmlSchema;
import net.hydromatic.filtex.lookml.LookmlSchemas;
import net.hydromatic.filtex.lookml.ObjectHandler;

import com.google.common.collect.ImmutableList;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static net.hydromatic.filtex.ParseFixture.minus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.fail;

/** Tests for the LookML event-based parser. */
public class LaxTest {
  private static void generateSampleEvents(ObjectHandler h) {
    h.obj("model", "m", h1 ->
            h1.number("n", 1)
                .string("s", "hello")
                .bool("b", true)
                .code("code", "VALUES 1")
                .list("list", h2 ->
                    h2.bool(false)
                        .number(-2.5)
                        .string("abc")
                        .list(h3 -> h3.string("singleton")))
                .list("emptyList", h2 -> {
                }))
        // This close() is spurious, but LookmlWriter currently needs it
        // to trigger a flush of the internal state.
        .close();
  }

  private static void assertParse(String s, Matcher<List<String>> matcher) {
    final ParseFixture.Parsed f = ParseFixture.of().parse(s);
    assertThat(f.list, matcher);
  }

  private static void assertParseThrows(String s, Matcher<Throwable> matcher) {
    try {
      final ParseFixture.Parsed f = ParseFixture.of().parse(s);
      fail("expected error, got " + f.list);
    } catch (RuntimeException e) {
      assertThat(e, matcher);
    }
  }

  @Test void testMinus() {
    final List<Integer> list = ImmutableList.of();
    final List<Integer> list123 = ImmutableList.of(1, 2, 3);
    final List<Integer> list1232 = ImmutableList.of(1, 2, 3, 2);
    final List<Integer> list2 = ImmutableList.of(2);
    final List<Integer> list13 = ImmutableList.of(1, 3);
    assertThat(minus(list123, list), hasToString("[1, 2, 3]"));
    assertThat(minus(list123, list13), hasToString("[2]"));
    assertThat(minus(list123, list2), hasToString("[1, 3]"));
    assertThat(minus(list1232, list2), hasToString("[1, 3, 2]"));
  }

  /** Creates a schema that is a subset of standard LookML. */
  static LookmlSchema coreSchema() {
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
                .addRefListProperty("drill_fields")
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

  /** Tests the LookML writer
   * {@link LaxHandlers#writer(StringBuilder, int, boolean)}
   * by running a sequence of parse events through the writer and checking
   * the generated LookML string. */
  @Test void testWriter() {
    final StringBuilder b = new StringBuilder();
    generateSampleEvents(LaxHandlers.writer(b, 2, true));
    final String lookml = "model: m {\n"
        + "  n: 1\n"
        + "  s: \"hello\"\n"
        + "  b: yes\n"
        + "  code: VALUES 1;;\n"
        + "  list: [\n"
        + "    no,\n"
        + "    -2.5,\n"
        + "    \"abc\",\n"
        + "    [\n"
        + "      \"singleton\"\n"
        + "    ]\n"
        + "  ]\n"
        + "  emptyList: []\n"
        + "}";
    assertThat(b, hasToString(lookml));

    // Same as previous, in non-pretty mode
    b.setLength(0);
    generateSampleEvents(LaxHandlers.writer(b, 2, false));
    final String lookml2 = "model:m {"
        + "n:1, s:\"hello\", b:yes, code:VALUES 1;;, "
        + "list:[no, -2.5, \"abc\", [\"singleton\"]], "
        + "emptyList:[]"
        + "}";
    assertThat(b, hasToString(lookml2));
  }

  /** Tests the logging handler
   * {@link net.hydromatic.filtex.lookml.LaxHandlers#logger}
   * by running a sequence of parser events through it and checking the
   * resulting list of strings. */
  @Test void testLogger() {
    final List<String> list = new ArrayList<>();
    generateSampleEvents(LaxHandlers.logger(list::add));
    assertThat(list,
        hasToString("[objOpen(model, m), "
            + "number(n, 1), "
            + "string(s, hello), "
            + "bool(b, true), "
            + "code(code, VALUES 1), "
            + "listOpen(list), "
            + "string(false), "
            + "number(-2.5), "
            + "string(abc), "
            + "listOpen(), "
            + "string(singleton), "
            + "listClose(), "
            + "listClose(), "
            + "listOpen(emptyList), "
            + "listClose(), "
            + "objClose()]"));
  }

  /** Tests that the same events come out of a filter handler
   * ({@link LaxHandlers#filter(ObjectHandler)}) as go into it. */
  @Test void testFilterHandler() {
    final List<String> list = new ArrayList<>();
    generateSampleEvents(LaxHandlers.logger(list::add));
    final List<String> list2 = new ArrayList<>();
    generateSampleEvents(LaxHandlers.filter(LaxHandlers.logger(list2::add)));
    assertThat(list2, hasSize(list.size()));
    assertThat(list2, hasToString(list.toString()));
  }

  @Test void testParse() {
    assertParse("model: m {}",
        hasToString("[objOpen(model, m),"
            + " objClose()]"));
    assertParseThrows("# just a comment",
        hasToString("java.lang.RuntimeException: "
            + "net.hydromatic.filtex.lookml.parse.ParseException: "
            + "Encountered \"<EOF>\" at line 1, column 16.\n"
            + "Was expecting:\n"
            + "    <IDENTIFIER> ...\n"
            + "    "));
    assertParseThrows("abc",
        hasToString("java.lang.RuntimeException: "
            + "net.hydromatic.filtex.lookml.parse.ParseException: "
            + "Encountered \"<EOF>\" at line 1, column 3.\n"
            + "Was expecting:\n"
            + "    \":\" ...\n"
            + "    "));
    assertParse("model: m {}",
        hasToString("[objOpen(model, m),"
            + " objClose()]"));
    assertParse("s: \"a \\\"quoted\\\" string\"",
        hasToString("[string(s, a \\\"quoted\\\" string)]"));
    assertParse("p: []",
        hasToString("[listOpen(p), listClose()]"));
    assertParse("p: [1]",
        hasToString("[listOpen(p), number(1), listClose()]"));
    assertParse("p: [1, true, [2], -3.5]",
        hasToString("[listOpen(p), number(1), identifier(true),"
            + " listOpen(), number(2), listClose(),"
            + " number(-3.5), listClose()]"));
    assertParse("# begin\n"
            + "model: m {\n"
            + "# middle\n"
            + "} # end",
        hasToString("[comment(# begin),"
            + " objOpen(model, m),"
            + " comment(# middle),"
            + " objClose(),"
            + " comment(# end)]"));
    assertParseThrows("",
        hasToString("java.lang.RuntimeException: "
            + "net.hydromatic.filtex.lookml.parse.ParseException: "
            + "Encountered \"<EOF>\" at line 0, column 0.\n"
            + "Was expecting:\n"
            + "    <IDENTIFIER> ...\n"
            + "    "));
    assertParse("model: m {\n"
            + "  sql: multi\n"
            + "     line;;\n"
            + "}",
        hasToString("[objOpen(model, m),"
            + " code(sql,  multi\n"
            + "     line),"
            + " objClose()]"));
    assertParse("model: m {\n"
            + "  my_list: [\n"
            + "    # before element 0\n"
            + "    0,\n"
            + "    # between elements 0 and 1\n"
            + "    # another\n"
            + "    1,\n"
            + "    2\n"
            + "    # after element but before comma\n"
            + "    ,\n"
            + "    # after comma\n"
            + "    2\n"
            + "    # after last element\n"
            + "  ]\n"
            + "}",
        hasToString("["
            + "objOpen(model, m),"
            + " listOpen(my_list),"
            + " comment(# before element 0),"
            + " number(0),"
            + " comment(# between elements 0 and 1),"
            + " comment(# another),"
            + " number(1),"
            + " number(2),"
            + " comment(# after element but before comma),"
            + " comment(# after comma),"
            + " number(2),"
            + " comment(# after last element),"
            + " listClose(),"
            + " objClose()"
            + "]"));
  }

  /** Tests building a simple schema with one enum type. */
  @Test void testSchemaBuilder() {
    LookmlSchema s =
        LookmlSchemas.schemaBuilder()
            .setName("simple")
            .addEnum("boolean", "true", "false")
            .build();
    assertThat(s.name(), is("simple"));
    assertThat(s.objectTypes(), anEmptyMap());
    assertThat(s.enumTypes(), aMapWithSize(1));
    assertThat(s.enumTypes().get("boolean").allowedValues(),
        hasToString("[false, true]"));
  }

  /** Tests building a schema with two enum types and one root object type. */
  @Test void testSchemaBuilder2() {
    LookmlSchema s =
        LookmlSchemas.schemaBuilder()
            .setName("example")
            .addEnum("boolean", "true", "false")
            .addEnum("join_type", "inner", "cross_join", "left_outer")
            .addNamedObjectProperty("empty_object",
                LookmlSchemas.ObjectTypeBuilder::build)
            .addNamedObjectProperty("model",
                b -> b.addNumberProperty("x")
                    .addStringProperty("y")
                    .addEnumProperty("z", "boolean")
                    .addObjectProperty("empty_object")
                    .build())
            .build();
    assertThat(s.name(), is("example"));
    assertThat(s.enumTypes(), aMapWithSize(2));
    assertThat(s.enumTypes().get("boolean").allowedValues(),
        hasToString("[false, true]"));
    assertThat(s.enumTypes().get("join_type").allowedValues(),
        hasToString("[cross_join, inner, left_outer]"));
    assertThat(s.objectTypes(), aMapWithSize(2));
    assertThat(s.objectTypes().get("baz"), nullValue());
    assertThat(s.objectTypes().get("model"), notNullValue());
    assertThat(s.objectTypes().get("model").properties().keySet(),
        hasToString("[empty_object, x, y, z]"));
  }

  /** Tests building a schema where the same property name ("sql") is used for
   * both code and non-code properties. */
  @Test void testSchemaBuilderFailsWithMixedCodeProperties() {
    try {
      LookmlSchema s =
          LookmlSchemas.schemaBuilder()
              .addObjectType("view",
                  b -> b.addNumberProperty("x")
                      .addCodeProperty("sql")
                      .build())
              .addObjectType("dimension",
                  b -> b.addNumberProperty("y")
                      .addStringProperty("sql")
                      .build())
              .build();
      fail("expected error, got " + s);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(),
          is("property 'sql' has both code and non-code uses"));
    }
  }

  /** Tests building core LookML schema. */
  @Test void testSchemaBuilder3() {
    final LookmlSchema schema = coreSchema();
    assertThat(schema.objectTypes(), aMapWithSize(7));
    assertThat(schema.enumTypes(), aMapWithSize(5));
    assertThat(schema.rootProperties(), aMapWithSize(1));
  }

  /** Validates a LookML document according to the core schema. */
  @Test void testValidateCore() {
    final LookmlSchema schema = coreSchema();
    final ParseFixture f = ParseFixture.of().withSchema(schema);
    final String s = "dimension: d {x: 1}";
    ParseFixture.Parsed f2 = f.parse(s);
    assertThat(f2.errorList,
        hasToString("[invalidRootProperty(dimension)]"));
    assertThat("all events should be discarded", f2.list, hasSize(0));

    ParseFixture.Parsed f3 = f.parse("model: {x: 1}");
    assertThat(f3.errorList,
        hasToString("[nameRequired(model)]"));
    assertThat("all events should be discarded", f3.list, hasSize(0));

    final Consumer<String> fn = m -> {
      ParseFixture.Parsed f4 = f.parse("model: " + m);
      assertThat(f4.errorList,
          hasToString("[invalidRootProperty(model)]"));
      assertThat("all events should be discarded", f4.list, hasSize(0));
    };
    fn.accept("1");
    fn.accept("\"a string\"");
    fn.accept("yes");
    fn.accept("inner_join");
    fn.accept("[]");

    ParseFixture.Parsed f4 = f.parse("model: m {\n"
        + "  dimension: d {}\n"
        + "}");
    assertThat(f4.errorList,
        hasToString("[invalidPropertyOfParent(dimension, model)]"));
    assertThat("d events should be discarded", f4.list, hasSize(2));
    assertThat(f4.list, hasToString("[objOpen(model, m), objClose()]"));

    ParseFixture.Parsed f5 = f.parse("model: m {\n"
        + "  view: v {\n"
        + "    dimension: d {\n"
        + "      sql: VALUES ;;\n"
        + "      type: number\n"
        + "      label: \"a label\"\n"
        + "      tags: 123\n"
        + "      drill_fields: true\n"
        + "    }\n"
        + "    measure: m {\n"
        + "      sql: VALUES 1;;\n"
        + "      type: average\n"
        + "      label: 1\n"
        + "    }\n"
        + "    dimension: d2 {\n"
        + "      type: average\n"
        + "      primary_key: \"a string\"\n"
        + "    }\n"
        + "    bad_object: {\n"
        + "      type: median\n"
        + "    }\n"
        + "  }\n"
        + "  explore: e {\n"
        + "    conditionally_filter: {\n"
        + "      filters: [f1: \"123\", f2: \"abc\"]\n"
        + "      unless: [f3, f4]\n"
        + "    }\n"
        + "  }\n"
        + "  explore: e2 {\n"
        + "    conditionally_filter: {\n"
        + "      bad: true\n"
        + "      filters: true\n"
        + "      unless: [\"a\", 1, f3, [2]]\n"
        + "    }\n"
        + "  }\n"
        + "}");
    assertThat(f5.errorList,
        hasToString("["
            + "invalidPropertyType(tags, STRING_LIST, NUMBER),"
            + " invalidPropertyType(drill_fields, REF_LIST, REF),"
            + " invalidPropertyType(label, STRING, NUMBER),"
            + " invalidPropertyType(dimension, type, "
            + "dimension_field_type, average),"
            + " invalidPropertyType(primary_key, ENUM, STRING),"
            + " invalidPropertyOfParent(bad_object, view),"
            + " invalidPropertyOfParent(bad, conditionally_filter),"
            + " invalidPropertyType(filters, REF_STRING_MAP, REF),"
            + " invalidListElement(unless, STRING, REF_LIST),"
            + " invalidListElement(unless, NUMBER, REF_LIST),"
            + " invalidListElement(unless, REF_LIST, REF_LIST)"
            + "]"));
    final List<String> discardedEvents = f5.discardedEvents();
    assertThat(discardedEvents, hasSize(15));
    assertThat(discardedEvents,
        hasToString("[number(tags, 123),"
            + " identifier(drill_fields, true),"
            + " number(label, 1),"
            + " identifier(type, average),"
            + " string(primary_key, a string),"
            + " objOpen(bad_object),"
            + " identifier(type, median),"
            + " objClose(),"
            + " identifier(bad, true),"
            + " identifier(filters, true),"
            + " string(a),"
            + " number(1),"
            + " listOpen(),"
            + " number(2),"
            + " listClose()]"));
  }

  /** Validates a LookML document that contains duplicate elements. */
  @Test void testValidateDuplicates() {
    final LookmlSchema schema = coreSchema();
    final ParseFixture f0 = ParseFixture.of().withSchema(schema);

    // Valid - no duplicates
    final String s = "model: m {\n"
        + "  explore: e1 {}\n"
        + "  explore: e2 {}\n"
        + "}";
    ParseFixture.Parsed f = f0.parse(s);
    assertThat(f.errorList, empty());

    // Invalid - duplicate explore e1
    final String s2 = "model: m {\n"
        + "  explore: e1 {}\n"
        + "  explore: e2 {}\n"
        + "  explore: e1 {}\n"
        + "}";
    f = f0.parse(s2);
    assertThat(f.errorList, hasSize(1));
    assertThat(f.errorList,
        hasToString("[duplicateNamedProperty(explore, e1)]"));
    assertThat(f.discardedEvents(), hasSize(2));
    assertThat(f.discardedEvents(),
        hasToString("[objOpen(explore, e1), objClose()]"));

    // Invalid - duplicate properties of type string, list, number, boolean,
    // enum (dimension.type)
    final String s3 = "model: m {\n"
        + "  fiscal_month_offset: 3\n"
        + "  view: v1 {\n"
        + "    label: \"label 1\"\n"
        + "    label: \"label 2\"\n"
        + "    dimension: d1{\n"
        + "      drill_fields: []\n"
        + "      primary_key: true\n"
        + "      type: date\n"
        + "      drill_fields: [f1]\n"
        + "      primary_key: true\n"
        + "      type: tier\n"
        + "    }\n"
        + "  }\n"
        + "  fiscal_month_offset: 2\n"
        + "}\n";
    f = f0.parse(s3);
    assertThat(f.errorList, hasSize(5));
    assertThat(f.errorList,
        hasToString("["
            + "duplicateProperty(label), "
            + "duplicateProperty(drill_fields), "
            + "duplicateProperty(primary_key), "
            + "duplicateProperty(type), "
            + "duplicateProperty(fiscal_month_offset)]"));
    assertThat(f.discardedEvents(), hasSize(7));
    assertThat(f.discardedEvents(),
        hasToString("["
            + "string(label, label 2), "
            + "listOpen(drill_fields), "
            + "identifier(f1), "
            + "listClose(), "
            + "identifier(primary_key, true), "
            + "identifier(type, tier), "
            + "number(fiscal_month_offset, 2)]"));
  }

  /** Tests that the schema-schema obtained by parsing {@code lkml-schema.lkml}
   * is equivalent to the one created by the {@link LookmlSchemas#schemaSchema()} method.
   * Also lets the schema-schema validate itself. */
  @Test void testCompareSchemaSchema() {
    final URL url = LaxTest.class.getResource("/lookml/lkml-schema.lkml");
    final LookmlSchema schema = LookmlSchemas.load(url, null);
    final LookmlSchema schemaSchema = LookmlSchemas.schemaSchema();
    assertThat(LookmlSchemas.compare(schema, schemaSchema), empty());
    assertThat(LookmlSchemas.equal(schema, schemaSchema), is(true));

    // Use the schema to validate itself.
    final LookmlSchema schema2 = LookmlSchemas.load(url, schema);
    assertThat(LookmlSchemas.equal(schema, schema2), is(true));
  }

  /** Tests that the core schema obtained by parsing {@code core-schema.lkml}
   * is equivalent to the one created by the {@link #coreSchema()} method. */
  @Test void testCompareCoreSchema() {
    final URL url = LaxTest.class.getResource("/lookml/core-schema.lkml");
    final LookmlSchema schema =
        LookmlSchemas.load(url, LookmlSchemas.schemaSchema());
    final LookmlSchema coreSchema = coreSchema();
    assertThat(LookmlSchemas.compare(schema, coreSchema), empty());
    assertThat(LookmlSchemas.equal(schema, coreSchema), is(true));
  }

  /** Builds a model. */
  @Test void testBuild() {
    final ParseFixture f0 = ParseFixture.of().withSchema(coreSchema());
    ParseFixture.Parsed f1 = f0.parse("model: m {\n"
        + "  view: v1 {}\n"
        + "  view: v2 {}\n"
        + "  explore: e {\n"
        + "    join: v2 {}"
        + "  }\n"
        + "}");
    assertThat(f1.errorList, empty());
    ParseFixture.Validated f2 = f1.validate();
    assertThat(f2.list, empty());
    assertThat(f2.model, notNullValue());
  }

  /** Validates a model. */
  @Test void testValidate() {
    final ParseFixture f0 = ParseFixture.of().withSchema(coreSchema());
    ParseFixture.Parsed f1 = f0.parse("model: m {\n"
        + "  view: v1 {}\n"
        + "  explore: e {\n"
        + "    view_name: v2"
        + "  }\n"
        + "}");
    assertThat(f1.errorList, empty());
    ParseFixture.Validated f2 = f1.validate();
    assertThat(f2.list, empty());
  }
}

// End LaxTest.java
