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
import net.hydromatic.filtex.lookml.ObjectHandler;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasToString;
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
    final List<String> list = new ArrayList<>();
    LaxParser.parse(s, LaxHandlers.logger(list::add));
    assertThat(list, matcher);
  }

  private static void assertParseThrows(String s, Matcher<Throwable> matcher) {
    try {
      final List<String> list = new ArrayList<>();
      LaxParser.parse(s, LaxHandlers.logger(list::add));
      fail("expected error, got " + list);
    } catch (RuntimeException e) {
      assertThat(e, matcher);
    }
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
  }
}

// End LaxTest.java
