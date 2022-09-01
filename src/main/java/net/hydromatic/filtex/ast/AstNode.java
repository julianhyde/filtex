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
package net.hydromatic.filtex.ast;

import org.checkerframework.checker.nullness.qual.Nullable;

/** Base class for Abstract Syntax Tree node. */
public abstract class AstNode {
  public final Pos pos;
  public final Op op;

  /** Unique identifier of a node, or null. It's mutable, so that we don't have
   * to copy the tree just to number it, but just be careful. */
  public @Nullable Integer id;

  protected AstNode(Pos pos, Op op) {
    this.pos = pos;
    this.op = op;
  }

  @Override public String toString() {
    return unparse(new AstWriter()).toString();
  }

  public abstract AstWriter unparse(AstWriter writer);

  public abstract void accept(AstVisitor visitor, @Nullable AstNode parent);

  /** False if negated, true if not negated. */
  public boolean is() {
    return true;
  }

  public @Nullable String expression() {
    return null;
  }

  public @Nullable String summary() {
    return null;
  }

  /** Returns the FilterModel type. For example, "," for "OR". */
  public String type() {
    return op.s;
  }

  @SuppressWarnings("rawtypes")
  public Iterable<Comparable> value() {
    return null;
  }

  public String low() {
    return null;
  }

  public String high() {
    return null;
  }

  public String bounds() {
    return null;
  }

  @SuppressWarnings("rawtypes")
  public @Nullable String valueString() {
    final @Nullable Iterable<Comparable> value = value();
    if (value == null) {
      return null;
    }
    final StringBuilder b = new StringBuilder();
    String sep = "";
    for (Comparable comparable : value) {
      b.append(sep).append(comparable);
      sep = ",";
    }
    return b.toString();
  }
}

// End AstNode.java
