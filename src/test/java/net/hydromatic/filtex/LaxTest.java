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
import net.hydromatic.filtex.lookml.LaxParser;
import net.hydromatic.filtex.lookml.LookmlSchema;
import net.hydromatic.filtex.lookml.LookmlSchemas;
import net.hydromatic.filtex.lookml.ObjectHandler;

import com.google.common.collect.ImmutableSortedSet;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.fail;

/** Tests for the LookML event-based parser. */
public class LaxTest {
  private static final Set<String> CODES = ImmutableSortedSet.of("sql");

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
    final List<String> list = new ArrayList<>();
    LaxParser.parse(LaxHandlers.logger(list::add), CODES, s);
    assertThat(list, matcher);
  }

  private static void assertParseThrows(String s, Matcher<Throwable> matcher) {
    try {
      final List<String> list = new ArrayList<>();
      LaxParser.parse(LaxHandlers.logger(list::add), CODES, s);
      fail("expected error, got " + list);
    } catch (RuntimeException e) {
      assertThat(e, matcher);
    }
  }

  /** Creates a schema that is a subset of standard LookML. */
  static LookmlSchema coreSchema() {
    return LookmlSchemas.schemaBuilder()
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
        .addObjectType("dimension", b ->
            b.addEnumProperty("type", "dimension_field_type")
                .build())
        .addObjectType("measure", b ->
            b.addEnumProperty("type", "measure_field_type")
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
                .build())
        .addNamedObjectProperty("model", b ->
            b.addNamedObjectProperty("explore")
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
            + "objClose(), "
            + "objClose()]"));
  }

  @Test void testParse() {
    assertParse("model: m {}",
        hasToString("[objOpen(model, m),"
            + " objClose()]"));
    assertParseThrows("# just a comment",
        hasToString("java.lang.RuntimeException: "
            + "net.hydromatic.filtex.lookml.parse.ParseException: "
            + "Encountered \"<EOF>\" at line 1, column 16.\n"
            + "Was expecting one of:\n"
            + "    <IDENTIFIER> ...\n"
            + "    <COMMENT> ...\n"
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
            + "Was expecting one of:\n"
            + "    <IDENTIFIER> ...\n"
            + "    <COMMENT> ...\n"
            + "    "));
    assertParse("model: m {\n"
            + "  sql: multi\n"
            + "     line;;\n"
            + "}",
        hasToString("[objOpen(model, m),"
            + " code(sql,  multi\n"
            + "     line),"
            + " objClose()]"));
  }

  /** Tests building a simple schema with one enum type. */
  @Test void testSchemaBuilder() {
    LookmlSchema s =
        LookmlSchemas.schemaBuilder()
            .addEnum("boolean", "true", "false")
            .build();
    assertThat(s.objectTypes(), anEmptyMap());
    assertThat(s.enumTypes(), aMapWithSize(1));
    assertThat(s.enumTypes().get("boolean").allowedValues(),
        hasToString("[false, true]"));
  }

  /** Tests building a schema with two enum types and one root object type. */
  @Test void testSchemaBuilder2() {
    LookmlSchema s =
        LookmlSchemas.schemaBuilder()
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
    assertThat(schema.objectTypes(), aMapWithSize(6));
    assertThat(schema.enumTypes(), aMapWithSize(5));
    assertThat(schema.rootProperties(), aMapWithSize(1));
  }

}

// End LaxTest.java
