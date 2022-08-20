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

import net.hydromatic.filtex.ast.AstNode;
import net.hydromatic.filtex.parse.FiltexParserImpl;
import net.hydromatic.filtex.parse.ParseException;

import org.hamcrest.Matcher;

import java.io.StringReader;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;

/** Fluent tester. */
public class Ft {
  public final TypeFamily typeFamily;
  public final String s;

  Ft(TypeFamily typeFamily, String s) {
    this.typeFamily = typeFamily;
    this.s = s;
  }

  /** Creates an {@code Ft}. */
  static Ft ft(TypeFamily typeFamily, String s) {
    return new Ft(typeFamily, s);
  }

  /** Creates a parser and performs the given action. */
  Ft withParser(Consumer<FiltexParserImpl> action) {
    final FiltexParserImpl parser = new FiltexParserImpl(new StringReader(s));
    action.accept(parser);
    return this;
  }

  Ft assertParse(Matcher<? extends AstNode> matcher) {
    return withParser(parser -> {
      try {
        final AstNode node;
        switch (typeFamily) {
        case NUMBER:
          node = parser.numericExpression();
          break;
        default:
          throw new AssertionError("unknown " + typeFamily);
        }
        //noinspection unchecked
        assertThat(node, (Matcher) matcher);
      } catch (ParseException e) {
        throw new RuntimeException(e);
      }
    });
  }
}

// End Ft.java
