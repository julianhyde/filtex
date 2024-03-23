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
package net.hydromatic.filtex.util;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/** Utilities for {@link Enum} and {@code enum}. */
public class Enums {
  private Enums() {}

  /** For a given enum class, returns a map from names to constants. */
  public static <E extends Enum<E>> Map<String, E> getConstants(
      Class<E> enumClass) {
    ImmutableMap.Builder<String, E> map = ImmutableMap.builder();
    for (E enumConstant : enumClass.getEnumConstants()) {
      map.put(enumConstant.name(), enumConstant);
    }
    return map.build();
  }
}

// End Enums.java
