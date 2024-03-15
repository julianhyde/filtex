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

/** Handler that forwards all events to a consumer.
 *
 * <p>As it stands, it is a no-op. But it is useful for subclassing. */
class FilterHandler implements ObjectHandler {
  final ObjectHandler consumer;

  FilterHandler(ObjectHandler consumer) {
    this.consumer = consumer;
  }

  @Override public ObjectHandler comment(String comment) {
    consumer.comment(comment);
    return this;
  }

  @Override public ObjectHandler number(String propertyName, Number value) {
    consumer.number(propertyName, value);
    return this;
  }

  @Override public ObjectHandler bool(String propertyName, boolean value) {
    consumer.bool(propertyName, value);
    return this;
  }

  @Override public ObjectHandler string(String propertyName, String value) {
    consumer.string(propertyName, value);
    return this;
  }

  @Override public ObjectHandler identifier(String propertyName, String value) {
    consumer.identifier(propertyName, value);
    return this;
  }

  @Override public ObjectHandler code(String propertyName, String value) {
    consumer.code(propertyName, value);
    return this;
  }

  @Override public ListHandler listOpen(String propertyName) {
    final ListHandler listHandler = consumer.listOpen(propertyName);
    return new FilterListHandler(listHandler);
  }

  @Override public ObjectHandler objOpen(String property) {
    final ObjectHandler objectHandler = consumer.objOpen(property);
    return new FilterHandler(objectHandler);
  }

  @Override public ObjectHandler objOpen(String propertyName, String name) {
    final ObjectHandler objectHandler = consumer.objOpen(propertyName, name);
    return new FilterHandler(objectHandler);
  }

  @Override public void close() {
    consumer.close();
  }

  /** Implementation of {@link ListHandler} that forwards to a consumer. */
  private static class FilterListHandler implements ListHandler {
    final ListHandler consumer;

    private FilterListHandler(ListHandler consumer) {
      this.consumer = consumer;
    }

    @Override public ListHandler string(String value) {
      consumer.string(value);
      return this;
    }

    @Override public ListHandler number(Number value) {
      consumer.number(value);
      return this;
    }

    @Override public ListHandler bool(boolean value) {
      consumer.bool(value);
      return this;
    }

    @Override public ListHandler identifier(String value) {
      consumer.identifier(value);
      return this;
    }

    @Override public ListHandler comment(String comment) {
      consumer.comment(comment);
      return this;
    }

    @Override public ListHandler listOpen() {
      final ListHandler listHandler = consumer.listOpen();
      return new FilterListHandler(listHandler);
    }

    @Override public void close() {
      consumer.close();
    }
  }
}
