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
import net.hydromatic.filtex.ast.Op;

import com.google.common.collect.Iterables;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.hydromatic.filtex.ast.AstBuilder.ast;

/** Transforms to be applied to ASTs after parsing. */
public class Transforms {
  private Transforms() {}

  /** Traverses ast and returns the count of 'not' nodes ('is' value set to
   * false). */
  static int countNots(AstNode root) {
    class CountingConsumer implements Consumer<Asts.Model> {
      private int count = 0;

      @Override public void accept(Asts.Model model) {
        if (!model.is) {
          ++count;
        }
      }
    }

    CountingConsumer consumer = new CountingConsumer();
    Asts.traverse(root, consumer);
    return consumer.count;
  }

  /** When two duplicate "is not" nodes are present,
   * removes the second one. */
  static AstNode removeDuplicateNotNodes(AstNode root) {
    final AstNode workingRoot = Asts.applyId(root);
    // get the andClauses - "is not" nodes from the ast
    final List<Asts.Model> andClauses =
        Asts.treeToList(workingRoot).stream().filter(model -> !model.is)
            .collect(Collectors.toList());
    // we only care if there are two andClauses with the same expression
    return andClauses.size() == 2
        && andClauses.get(0).equals(andClauses.get(1))
        ? // remove the second one
        Asts.removeNode(workingRoot, andClauses.get(1).id)
        : workingRoot;
  }

  /** Merges the value array of two nodes, removing duplicates. */
  static AstNode mergeNodes(AstNode left, AstNode right) {
    assert left.op == right.op;
    switch (left.op) {
    case EQ:
      final Ast.Comparison left2 = (Ast.Comparison) left;
      final Ast.Comparison right2 = (Ast.Comparison) right;
      return ast.comparison(left2.is & right2.is, left.op,
          Iterables.concat(left2.value, right2.value));

    default:
      throw new AssertionError(left.op);
    }
  }

  /** Returns whether the left && right.left children of this node are of the
   * same type and can be merged. */
  static boolean canMergeLeftNodes(Ast.@Nullable Comparison left,
      Ast.@Nullable Call2 right, Op compareType,
      boolean allowDifferentIsValue) {
    return left != null
        && right != null
        && right.left != null
        && left.op == right.left.op
        && left.op == compareType
        && (left.model().is == right.left.model().is || allowDifferentIsValue);
  }

  /** Returns whether the left && right children of node are of the same type
   * and can be merged. */
  static boolean canMergeEndNodes(Ast.@Nullable Comparison left,
      Ast.@Nullable Comparison right, Op compareType,
      boolean allowDifferentIsValue) {
    return left != null
        && right != null
        && left.op == right.op
        && left.op == compareType
        && (left.model().is == right.is || allowDifferentIsValue);
  }

  /** Traverses AST and merges nodes of the same multi value type. */
  static AstNode mergeNodesWithSameType(AstNode root, Op compareType,
      boolean allowDifferentIsValue) {
    AstNode node = root;

    while (node instanceof Ast.Call2
        && ((Ast.Call2) node).left instanceof Ast.Comparison
        && ((Ast.Call2) node).right instanceof Ast.Call2
        && canMergeLeftNodes((Ast.Comparison) ((Ast.Call2) node).left,
            (Ast.Call2) ((Ast.Call2) node).right, compareType,
            allowDifferentIsValue)) {
      final Ast.Call2 node2 = (Ast.Call2) node;
      final Ast.Comparison left = (Ast.Comparison) node2.left;
      final Ast.Call2 right = (Ast.Call2) node2.right;
      final AstNode newLeft = mergeNodes(left, right.left);
      final AstNode newRight = right.right;
      node = new Ast.Call2(node2.op, newLeft, newRight);
    }

    if (node instanceof Ast.Call2
        && ((Ast.Call2) node).left instanceof Ast.Comparison
        && ((Ast.Call2) node).right instanceof Ast.Comparison
        && canMergeEndNodes((Ast.Comparison) ((Ast.Call2) node).left,
            (Ast.Comparison) ((Ast.Call2) node).right,
            compareType, allowDifferentIsValue)) {
      final Ast.Call2 node2 = (Ast.Call2) node;
      node = mergeNodes(node2.left, node2.right);
    }
    return node;
  }

  /** Transforms the AST by combining sequential nodes of the same type into a
   * single node. Used for merging number('=') and string('match') nodes of same
   * type. */
  public static AstNode mergeMultiValueNodes(AstNode root, Op type,
      boolean mergeDifferentIsValue) {
    final AstNode workingRoot =
        mergeNodesWithSameType(root, type, mergeDifferentIsValue);
    AstNode pointer = workingRoot;
    while (pointer instanceof Ast.Call2
        && ((Ast.Call2) pointer).right != null) {
      ((Ast.Call2) pointer).right =
          mergeNodesWithSameType(((Ast.Call2) pointer).right, type,
              mergeDifferentIsValue);
      pointer = ((Ast.Call2) pointer).right;
    }
    return workingRoot;
  }

  /**
   * Applies the following transformations on the number AST:
   *
   * <ul>
   *   <li>combine the value array on nodes of type '='</li>
   * </ul>
   */
  public static AstNode numberTransform(AstNode root) {
    // workaround for inconsistency in number filter and allow merging of nodes
    // with different 'is' value: 1, not 2 -> becomes
    // a single filter node { type: '=', is: false, value: [1, 2] }
    final int countOfNotNodes = countNots(root);
    final boolean mergeNodesWithDifferentIsValue = countOfNotNodes == 1;

    final AstNode mergedRoot =
        mergeMultiValueNodes(root,
            Op.EQ,
            mergeNodesWithDifferentIsValue);

    // if there are two "is not" nodes check if they are duplicates
    // to undo the "fix" applied when serializing the number filter
    final boolean checkForDuplicates = countOfNotNodes == 2;
    return checkForDuplicates
        ? removeDuplicateNotNodes(mergedRoot)
        : mergedRoot;
  }

}
