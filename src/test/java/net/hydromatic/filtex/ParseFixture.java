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

import net.hydromatic.filtex.lookml.ErrorHandler;
import net.hydromatic.filtex.lookml.LaxHandlers;
import net.hydromatic.filtex.lookml.LaxParser;
import net.hydromatic.filtex.lookml.LookmlSchema;
import net.hydromatic.filtex.lookml.ObjectHandler;

import com.google.common.collect.ImmutableSortedSet;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Contains necessary state for testing the parser and validator.
 */
class ParseFixture {
  final @Nullable LookmlSchema schema;
  final Set<String> codePropertyNames;

  private ParseFixture(@Nullable LookmlSchema schema,
      Set<String> codePropertyNames) {
    this.schema = schema;
    this.codePropertyNames = codePropertyNames;
  }

  /** Creates a ParseFixture. */
  static ParseFixture of() {
    return new ParseFixture(null, ImmutableSortedSet.of("sql"));
  }

  /** Returns a ParseFixture that is a copy of this with a given schema. */
  ParseFixture withSchema(LookmlSchema schema) {
    return new ParseFixture(requireNonNull(schema), schema.codePropertyNames());
  }

  /** Assigns the current LookML string and parses;
   * if {@link #schema} is not null, also validates. */
  Parsed parse(String s) {
    final List<String> list = new ArrayList<>();
    final List<String> errorList = new ArrayList<>();
    final ObjectHandler logger = LaxHandlers.logger(list::add);
    if (schema != null) {
      final ErrorHandler errorHandler =
          LaxHandlers.errorLogger(errorList::add);
      final ObjectHandler validator =
          LaxHandlers.validator(logger, schema, errorHandler);
      LaxParser.parse(validator, codePropertyNames, s);
    } else {
      LaxParser.parse(logger, codePropertyNames, s);
    }
    return new Parsed(this, list, errorList, s);
  }

  /** Subtracts one list from another in a merge-like manner. */
  static <E> List<E> minus(List<E> list0, List<E> list1) {
    List<E> list = new ArrayList<>();
    for (int i0 = 0, i1 = 0; i0 < list0.size();) {
      if (i1 < list1.size() && list0.get(i0).equals(list1.get(i1))) {
        // This element is in both. Skip it in both.
        ++i0;
        ++i1;
      } else {
        list.add(list0.get(i0));
        ++i0;
      }
    }
    return list;
  }

  static class Parsed {
    final ParseFixture parseFixture;
    final List<String> list;
    final List<String> errorList;
    final String s;

    Parsed(ParseFixture parseFixture, List<String> list, List<String> errorList,
        String s) {
      this.parseFixture = parseFixture;
      this.list = list;
      this.errorList = errorList;
      this.s = s;
    }

    /** Returns a list of events that are emitted without validation
     * but omitted with validation. */
    List<String> discardedEvents() {
      final List<String> list2 = new ArrayList<>();
      final ObjectHandler logger = LaxHandlers.logger(list2::add);
      LaxParser.parse(logger, parseFixture.codePropertyNames, s);
      return minus(list2, list);
    }
  }
}

// End ParseFixture.java