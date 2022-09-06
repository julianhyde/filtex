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

/**
 * Basic implementation of {@link AstVisitor}
 * that recursively visits children.
 */
public abstract class AstVisitorImpl implements AstVisitor {
  /** Called after the first child and before the second child of a node with
   * two children. */
  public void infix(Ast.Call2 call2, @Nullable AstNode parent) {
  }

  @Override public void visit(Ast.Call2 call2, @Nullable AstNode parent) {
    if (call2.left != null) {
      call2.left.accept(this, call2);
    }
    infix(call2, parent);
    if (call2.right != null) {
      call2.right.accept(this, call2);
    }
  }

  @Override public void visit(Ast.Call1 call1, @Nullable AstNode parent) {
    if (call1.node != null) {
      call1.node.accept(this, call1);
    }
  }

  @Override public void visit(Ast.Call0 call0, @Nullable AstNode parent) {
  }

  @Override public void visit(Ast.Comparison literal,
      @Nullable AstNode parent) {
  }

  @Override public void visit(Ast.NumericRange range,
      @Nullable AstNode parent) {
  }

  @Override public void visit(Ast.MatchesAdvanced matchesAdvanced,
      @Nullable AstNode parent) {
  }
}

// End AstVisitorImpl.java
