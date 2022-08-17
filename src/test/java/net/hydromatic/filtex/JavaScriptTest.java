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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static java.util.Objects.requireNonNull;

/** Test that invokes JavaScript via GraalVM. */
public class JavaScriptTest {

  private static final String TSC =
      "https://raw.githubusercontent.com/microsoft/TypeScript/main/lib/tsc.js";
  private static final String ROOT = "/Users/julianhyde/dev/";
  private static final String FILTER_EXPRESSIONS =
      "looker-components/packages/filter-expressions/src/index.ts";

  @Test void testJavaScript() throws IOException {
    final Context ctx =
        Context.newBuilder("js")
            // be careful with host access if you do not trust the source of
            // your JS files
            .allowAllAccess(true)
            .option("js.strict", "true")
            .build();

    // get a reference to the script container
    URL resource =
        JavaScriptTest.class.getClassLoader().getResource("barebones.js");
    requireNonNull(resource);
    final Source js =
        Source.newBuilder("js", resource).build();
    // make the engine evaluate the javascript script
    ctx.eval(js);
    // get a reference to the map function
    final Value mapFunc = ctx.getBindings("js").getMember("map");

    // execute the function, with the provided context
    final Value result =
        mapFunc.execute(new MappingContext("Mauro", "value"));
    // map the result to our type (whose object was created in the JS script)
    final MappingResult typedResult =
        result.as(new TypeLiteral<MappingResult>() {
        });

    // print the result
    System.out.println(typedResult);
  }

  @Disabled
  @Test void testTsc() throws IOException {
    final Context ctx =
        Context.newBuilder("js")
            // be careful with host access if you do not trust the source of
            // your JS files
            .allowAllAccess(true)
            .option("js.strict", "true")
            .build();

    // get a reference to the script container
    URL resource = new URL(TSC);
    requireNonNull(resource);
    final Source js =
        Source.newBuilder("js", resource).build();
    // make the engine evaluate the javascript script
    ctx.eval(js);
  }

  @Disabled
  @Test void testFilterExpressions() throws IOException {
    final Context ctx =
        Context.newBuilder("js")
            // be careful with host access if you do not trust the source of
            // your JS files
            .allowAllAccess(true)
            .option("js.strict", "true")
            .build();

    // get a reference to the script container
    File resource = new File(ROOT + FILTER_EXPRESSIONS);
    requireNonNull(resource);
    final Source js =
        Source.newBuilder("js", resource).build();
    // make the engine evaluate the javascript script
    ctx.eval(js);

    // get a reference to the map function
    final Value mapFunc = ctx.getBindings("js").getMember("map");

    // execute the function, with the provided context
    final Value result =
        mapFunc.execute(new MappingContext("Mauro", "value"));
    // map the result to our type (whose object was created in the JS script)
    final MappingResult typedResult =
        result.as(new TypeLiteral<MappingResult>() {
        });

    // print the result
    System.out.println(typedResult);
  }


  public static class MappingContext {
    private final String name;
    private final String value;

    public MappingContext(String name, String value) {
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }
  }

  public static class MappingResult {
    private final Object result;

    public MappingResult(Object result) {
      this.result = result;
    }

    @Override public String toString() {
      return "MappingResult{"
          + "result=" + result
          + '}';
    }
  }
}
