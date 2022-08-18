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

import java.math.BigDecimal;

/** AST Builder. */
public enum AstBuilder {
  /** The singleton instance of the AST builder.
   * The short name is convenient for use via 'import static',
   * but checkstyle does not approve. */
  // CHECKSTYLE: IGNORE 1
  ast;

  /** Creates an {@code int} literal. */
  public Ast.Literal intLiteral(Pos pos, BigDecimal value) {
    return new Ast.Literal(pos, value);
  }

  /** Creates a string literal. */
  public Ast.Literal stringLiteral(Pos pos, String value) {
    return new Ast.Literal(pos, value);
  }

  /** Creates a logical expression. */
  public AstNode logicalExpression(AstNode left, AstNode right) {
    return new Ast.Call(Op.COMMA, left, right);
  }
}

// End AstBuilder.java
