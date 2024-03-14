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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Writes fragments of LookML to a {@link StringBuilder},
 * and keeps track of the current indentation level.*/
class LookmlWriter {
  /**
   * Regex pattern for an identifier that does not need to be quoted.
   */
  static final Pattern SIMPLE_IDENTIFIER_PATTERN =
      Pattern.compile("[a-zA-Z0-9_]*");

  private final boolean pretty;
  private int indent = 0;
  private final int offset;
  final StringBuilder buf;

  LookmlWriter(boolean pretty, StringBuilder buf, int offset) {
    this.pretty = pretty;
    this.buf = buf;
    this.offset = offset;
  }

  /**
   * Adds a string value.
   */
  void string(String s) {
    buf.append('"').append(s.replace("\"", "\\s")).append('"');
  }

  /**
   * Adds a boolean value.
   */
  void bool(boolean b) {
    buf.append(b ? "yes" : "no");
  }

  /**
   * Adds a numeric value.
   */
  void number(Number number) {
    buf.append(number);
  }

  /**
   * Adds a code value.
   */
  void code(String s) {
    buf.append(s).append(";;");
  }

  /**
   * Adds an object.
   */
  void obj(PairList<String, Value> properties) {
    if (properties.isEmpty()) {
      buf.append("{}");
    } else {
      buf.append('{');
      if (pretty) {
        buf.append('\n');
        indent += offset;
        Spaces.append(buf, indent);
      }
      propertyList(properties);
      if (pretty) {
        buf.append('\n');
        indent -= offset;
        Spaces.append(buf, indent);
      }
      buf.append('}');
    }
  }

  /**
   * Adds a property list (an object without braces).
   */
  void propertyList(PairList<String, Value> properties) {
    properties.forEachIndexed((i, property, value) -> {
      if (i > 0) {
        if (pretty) {
          buf.append('\n');
          Spaces.append(buf, indent);
        } else {
          buf.append(", ");
        }
      }
      value.write(property, this);
    });
  }

  void label(String name) {
    identifier(name);
    buf.append(pretty ? ": " : ":");
  }

  void identifier(String id) {
    Matcher m = SIMPLE_IDENTIFIER_PATTERN.matcher(id);
    if (m.matches()) {
      buf.append(id);
    } else {
      string(id);
    }
  }

  /**
   * Adds a list value.
   */
  void list(List<Value> list) {
    if (list.isEmpty()) {
      buf.append("[]");
      return;
    }
    buf.append('[');
    indent += offset;
    for (int i = 0; i < list.size(); i++) {
      Value value = list.get(i);
      if (i > 0) {
        if (pretty) {
          buf.append(",\n");
          Spaces.append(buf, indent);
        } else {
          buf.append(", ");
        }
      } else {
        if (pretty) {
          buf.append('\n');
          Spaces.append(buf, indent);
        }
      }
      value.write(this);
    }
    indent -= offset;
    if (pretty) {
      buf.append('\n');
      Spaces.append(buf, indent);
    }
    buf.append(']');
  }
}

// End LookmlWriter.java
