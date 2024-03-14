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
import net.hydromatic.filtex.lookml.ObjectHandler;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasToString;

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

    b.setLength(0);
    generateSampleEvents(LaxHandlers.writer(b, 2, false));
    final String lookml2 = "model:m {"
        + "n:1, s:\"hello\", b:yes, code:VALUES 1;;, "
        + "list:[no, -2.5, \"abc\", [\"singleton\"]], "
        + "emptyList:[]"
        + "}";
    assertThat(b, hasToString(lookml2));
  }

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
}

// End LaxTest.java
