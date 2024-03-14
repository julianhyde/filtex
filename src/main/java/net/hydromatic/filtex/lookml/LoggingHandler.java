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

import java.util.function.Consumer;

class LoggingHandler implements ObjectHandler {
  private final Consumer<String> consumer;
  private final ListHandler listHandler = new LoggingListHandler();

  LoggingHandler(Consumer<String> consumer) {
    this.consumer = consumer;
  }

  @Override public ObjectHandler comment(String comment) {
    consumer.accept("comment(" + comment + ")");
    return this;
  }

  @Override public ObjectHandler number(String property, Number value) {
    consumer.accept("number(" + property + ", " + value + ")");
    return this;
  }

  @Override public ObjectHandler bool(String property, boolean value) {
    consumer.accept("bool(" + property + ", " + value + ")");
    return this;
  }

  @Override public ObjectHandler string(String property, String value) {
    consumer.accept("string(" + property + ", " + value + ")");
    return this;
  }

  @Override public ObjectHandler identifier(String property, String value) {
    consumer.accept("identifier(" + property + ", " + value + ")");
    return this;
  }

  @Override public ObjectHandler code(String property, String value) {
    consumer.accept("code(" + property + ", " + value + ")");
    return this;
  }

  @Override public ListHandler listOpen(String property) {
    consumer.accept("listOpen(" + property + ")");
    return listHandler;
  }

  @Override public ObjectHandler objOpen(String property) {
    consumer.accept("objOpen(" + property + ")");
    return this;
  }

  @Override public ObjectHandler objOpen(String property, String name) {
    consumer.accept("objOpen(" + property + ", " + name + ")");
    return this;
  }

  @Override public void close() {
    consumer.accept("objClose()");
  }

  /** Implementation of {@link net.hydromatic.filtex.lookml.ListHandler}
   * that logs events, as strings, to a consumer. */
  private class LoggingListHandler implements ListHandler {
    @Override public ListHandler string(String value) {
      consumer.accept("string(" + value + ")");
      return this;
    }

    @Override public ListHandler number(Number value) {
      consumer.accept("number(" + value + ")");
      return this;
    }

    @Override public ListHandler bool(boolean value) {
      consumer.accept("string(" + value + ")");
      return this;
    }

    @Override public ListHandler identifier(String value) {
      consumer.accept("identifier(" + value + ")");
      return this;
    }

    @Override public ListHandler comment(String comment) {
      consumer.accept("comment(" + comment + ")");
      return this;
    }

    @Override public ListHandler listOpen() {
      consumer.accept("listOpen()");
      return this;
    }

    @Override public void close() {
      consumer.accept("listClose()");
    }
  }
}
