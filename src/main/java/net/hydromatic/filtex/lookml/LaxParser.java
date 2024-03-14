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

import java.io.StringReader;

/** LookML parser that sends a sequence of events to a consumer. */
public class LaxParser {
  private LaxParser() {}

  /** State of being inside an object. */
  private static final int IN_OBJ = 0;

  /** State of being inside an identifier. */
  private static final int IN_ID = 1;

  /** State of being inside a comment. */
  private static final int COMMENT = 2;

  private static final String[] STATE_NAMES = {
      "IN_OBJ", "IN_ID", "COMMENT"};

  private static void err(char c, int state) {
    throw new IllegalArgumentException("unexpected char '" + c
        + " in state " + STATE_NAMES[state]);
  }

  public static void parse(String s, ObjectHandler handler) {
    if (true) {
      final LookmlParserImpl parser = new LookmlParserImpl(new StringReader(s));
      try {
        parser.document(handler);
      } catch (ParseException e) {
        throw new RuntimeException(e);
      }
      return;
    }
    // At start of doc, state is as if we just saw a '{' and entered an object.
    int state = IN_OBJ;
    int start; // ordinal of start of current identifier or comment
    int i = 0;
    final int size = s.length();
    for (; i < size;) {
      char c = s.charAt(i);
      switch (state) {
      case IN_OBJ:
        switch (c) {
        case '#':
          start = i++;
          for (;;) {
            if (i >= size) {
              break;
            }
            c = s.charAt(i++);
            if (c == '\n') {
              break;
            }
          }
          handler.comment(s.substring(start, i));
          if (i >= size) {
            return;
          }
          break;

        case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
        case 'g': case 'h': case 'i': case 'j': case 'k': case 'l':
        case 'm': case 'n': case 'o': case 'p': case 'q': case 'r':
        case 's': case 't': case 'u': case 'v': case 'w': case 'x':
        case 'y': case 'z':
          // We are looking at an identifier, e.g.
          //   abc: 1
          //   ^
          start = i++;
          for (;;) {
            if (i >= size) {
              break;
            }
            c = s.charAt(i++);
            if (c == '\n') {
              break;
            }
          }
          String id = s.substring(start, i);
          if (i >= size) {
            throw new IllegalArgumentException("trailing identifier " + id);
          }
          break;

        default:
          err(c, state);
        }
        break;

      case COMMENT:

      }
    }
  }
}

// End LaxParser.java
