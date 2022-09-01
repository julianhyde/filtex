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

import net.hydromatic.filtex.ast.Ast;
import net.hydromatic.filtex.ast.AstNode;
import net.hydromatic.filtex.ast.Asts;
import net.hydromatic.filtex.ast.Bound;

import com.google.common.collect.ImmutableList;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static net.hydromatic.filtex.ast.AstBuilder.ast;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/** Tests utilities. */
public class UtilTest {
  // Can convert an ast to array
  @Test void testTreeToList() {
    // id: 1, type: '=', value: [1]
    AstNode root = ast.numberLiteral(true, BigDecimal.ONE);
    List<AstNode> list = Asts.treeToList(root);
    assertThat(list.size(), is(1));
    assertThat(list.get(0), is(root));
  }

  // Tree list only holds value nodes
  @Test void testTreeToList2() {
    final AstNode root =
        ast.logicalExpression(
            ImmutableList.of(
                ast.numberLiteral(true, BigDecimal.ONE),
                ast.between(true, Bound.OPEN, Bound.ABSENT,
                    BigDecimal.valueOf(5), null)));
    final List<AstNode> list = Asts.treeToList(root);
    assertThat(list.size(), is(2));
    final Ast.Call2 call2 = (Ast.Call2) root;
    assertThat(list.get(0), is(call2.left));
    assertThat(list.get(1), is(call2.right));
  }

  // Tree list is sorted by the 'is' value of nodes
  @Test void testTreeToList3() {
    final AstNode root =
        ast.logicalExpression(
            ImmutableList.of(
                ast.numberLiteral(false, BigDecimal.ONE),
                ast.between(true, Bound.OPEN, Bound.ABSENT,
                    BigDecimal.valueOf(5), null)));
    final List<AstNode> list = Asts.treeToList(root);
    assertThat(list.size(), is(2));
    final Ast.Call2 call2 = (Ast.Call2) root;
    // right becomes before left. It all depends on what the value of 'is' is
    assertThat(list.get(0), is(call2.right));
    assertThat(list.get(1), is(call2.left));
  }
}

// End UtilTest.java
