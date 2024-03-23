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

import net.hydromatic.filtex.lookml.parse.LookmlParserImpl;
import net.hydromatic.filtex.lookml.parse.ParseException;

import com.google.common.collect.ImmutableSortedSet;

import java.io.StringReader;
import java.net.URL;
import java.util.Collection;

import static net.hydromatic.filtex.lookml.LookmlSchemas.urlContents;

/** LookML parser that sends a sequence of events to a consumer. */
public class LaxParser {
  private LaxParser() {}

  /** Parses a LookML string. */
  public static void parse(ObjectHandler handler,
      Collection<String> codePropertyNames, String s) {
    final LookmlParserImpl parser = new LookmlParserImpl(new StringReader(s));
    parser.setCodePropertyNames(ImmutableSortedSet.copyOf(codePropertyNames));
    try {
      parser.document(handler);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
    handler.close();
  }

  /** Parses a LookML string. */
  public static void parse(ObjectHandler handler,
      Collection<String> codePropertyNames, URL url) {
    final String s = urlContents(url);
    parse(handler, codePropertyNames, s);
  }
}

// End LaxParser.java
