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

/** Handler that converts LookML parse events into strings, and appends those
 * strings to a given consumer. */
class LoggingHandler implements ObjectHandler {
  protected final Consumer<String> consumer;
  protected final ListHandler listHandler;

  static ObjectHandler create(Consumer<String> consumer) {
    return new RootLoggingHandler(consumer, new LoggingListHandler(consumer));
  }

  private LoggingHandler(Consumer<String> consumer, ListHandler listHandler) {
    this.consumer = consumer;
    this.listHandler = listHandler;
  }

  @Override public ObjectHandler comment(String comment) {
    consumer.accept("comment(" + comment + ")");
    return this;
  }

  @Override public ObjectHandler number(String propertyName, Number value) {
    consumer.accept("number(" + propertyName + ", " + value + ")");
    return this;
  }

  @Override public ObjectHandler bool(String propertyName, boolean value) {
    consumer.accept("bool(" + propertyName + ", " + value + ")");
    return this;
  }

  @Override public ObjectHandler string(String propertyName, String value) {
    consumer.accept("string(" + propertyName + ", " + value + ")");
    return this;
  }

  @Override public ObjectHandler identifier(String propertyName, String value) {
    consumer.accept("identifier(" + propertyName + ", " + value + ")");
    return this;
  }

  @Override public ObjectHandler code(String propertyName, String value) {
    consumer.accept("code(" + propertyName + ", " + value + ")");
    return this;
  }

  @Override public ListHandler listOpen(String propertyName) {
    consumer.accept("listOpen(" + propertyName + ")");
    return listHandler;
  }

  @Override public ObjectHandler objOpen(String property) {
    consumer.accept("objOpen(" + property + ")");
    return this;
  }

  @Override public ObjectHandler objOpen(String propertyName, String name) {
    consumer.accept("objOpen(" + propertyName + ", " + name + ")");
    return this;
  }

  @Override public void close() {
    consumer.accept("objClose()");
  }

  /** Handler for the root of the document. Its behavior is as
   * {@link LoggingHandler} except that {@link #close()} does not generate a
   * message. */
  private static class RootLoggingHandler extends LoggingHandler {
    RootLoggingHandler(Consumer<String> consumer,
        LoggingListHandler loggingListHandler) {
      super(consumer, loggingListHandler);
    }

    @Override public ObjectHandler objOpen(String propertyName) {
      consumer.accept("objOpen(" + propertyName + ")");
      return new LoggingHandler(consumer, listHandler);
    }

    @Override public ObjectHandler objOpen(String propertyName, String name) {
      consumer.accept("objOpen(" + propertyName + ", " + name + ")");
      return new LoggingHandler(consumer, listHandler);
    }

    @Override public void close() {
      // swallows the 'onClose()' message
    }
  }

  /** Implementation of {@link net.hydromatic.filtex.lookml.ListHandler}
   * that logs events, as strings, to a consumer.
   *
   * <p>This class is necessary because there are methods in common between
   * the {@link ObjectHandler} and {@link ListHandler} interfaces. If there
   * were no methods in common, a single object could have implemented both
   * interfaces. */
  private static class LoggingListHandler implements ListHandler {
    private final Consumer<String> consumer;

    LoggingListHandler(Consumer<String> consumer) {
      super();
      this.consumer = consumer;
    }

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

    @Override public ListHandler pair(String ref, String identifier) {
      consumer.accept("pair(" + ref + ", " + identifier + ")");
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
