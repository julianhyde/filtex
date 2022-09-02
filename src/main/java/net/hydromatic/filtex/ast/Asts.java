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

import net.hydromatic.filtex.TypeFamily;

import com.google.common.collect.ImmutableList;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.hydromatic.filtex.ast.AstBuilder.ast;

/** Utilities for AST nodes. */
public class Asts {
  private Asts() {}

  /**
   * Converts a FilterAST to a list of FilterASTNodes using in order traversal
   * (left, node, right)
   *
   * <pre>{@code
   *    (root(1))      ->  [0,1,2,3,4]
   *    /      \
   * left(0)   right(3)
   *           /     \
   *      left(2)     right(4)
   * }</pre>
   *
   * <p>See
   * <a href="https://github.com/looker-open-source/components/blob/main/packages/filter-expressions/src/utils/tree/tree_to_list.ts">
   * tree_to_list.ts</a>
   */
  public static List<AstNode> treeToList(AstNode root) {
    final List<AstNode> orItems = new ArrayList<>();
    final List<AstNode> andItems = new ArrayList<>();
    final AstVisitor visitor =
        new AstVisitorImpl() {
          @Override public void infix(Ast.Call2 call2,
              @Nullable AstNode parent) {
            if (call2.op != Op.COMMA) {
              (call2.is() ? orItems : andItems).add(call2);
            }
          }

          @Override public void visit(Ast.Call0 call0,
              @Nullable AstNode parent) {
            (call0.is() ? orItems : andItems).add(call0);
          }

          @Override public void visit(Ast.MatchesAdvanced matchesAdvanced,
              @Nullable AstNode parent) {
            (matchesAdvanced.is() ? orItems : andItems).add(matchesAdvanced);
          }

          @Override public void visit(Ast.Comparison literal,
              @Nullable AstNode parent) {
            (literal.is() ? orItems : andItems).add(literal);
          }

          @Override public void visit(Ast.Range range,
              @Nullable AstNode parent) {
            List<AstNode> list = range.is ? orItems : andItems;
            if (range.left != null && range.right != null) {
              list.add(range);
            } else if (range.left != null) {
              Op op = range.op.containsLowerBound() ? Op.GE : Op.GT;
              AstNode comparison = ast.comparison(true, op, range.left);
              list.add(comparison);
            } else if (range.right != null) {
              Op op = range.op.containsUpperBound() ? Op.LE : Op.LT;
              AstNode comparison = ast.comparison(true, op, range.right);
              list.add(comparison);
              // TODO list.add(model(range.right));
            }
          }
        };
    root.accept(visitor, null);
    return ImmutableList.<AstNode>builder()
        .addAll(orItems)
        .addAll(andItems)
        .build();
  }

  /** Given an AST and a nodeToString conversion function for that particular
   * type of filter, it converts the AST to a string expression representation.
   *
   * <p>See
   * <a href="https://github.com/looker-open-source/components/blob/main/packages/filter-expressions/src/utils/tree/tree_to_string.ts">
   * tree_to_string.ts</a>. */
  public static String treeToString(AstNode root, Function<AstNode, String> f,
      Predicate<AstNode> predicate) {
    return treeToList(root).stream()
        .filter(predicate)
        .map(f)
        .collect(Collectors.joining(","));
  }

  /** Traverses the tree depth-first inorder (left, root, right) and assigns an
   * id attribute to each node.
   *
   * <p>See
   * <a href="https://github.com/looker-open-source/components/blob/main/packages/filter-expressions/src/utils/tree/inorder_traversal.ts">
   * inorder_traversal.ts</a>.
   */
  static void inorderTraversal(AstNode root, NodeHandler<Void> nodeHandler) {
    inorder(root, null, nodeHandler);
  }

  private static void inorder(AstNode node, AstNode parent,
      NodeHandler<Void> nodeHandler) {
    if (node instanceof Ast.Call2) {
      Ast.Call2 call2 = (Ast.Call2) node;
      if (call2.left != null) {
        inorder(call2.left, node, nodeHandler);
      }
      nodeHandler.apply(node, parent);
      if (call2.right != null) {
        inorder(call2.right, node, nodeHandler);
      }
    } else if (node instanceof Ast.Range) {
      Ast.Range call2 = (Ast.Range) node;
      if (call2.left != null) {
        inorder(ast.numberLiteral(true, call2.left), node, nodeHandler);
      }
      nodeHandler.apply(node, parent);
      if (call2.right != null) {
        inorder(ast.numberLiteral(true, call2.right), node, nodeHandler);
      }
    } else if (node instanceof Ast.Comparison) {
      nodeHandler.apply(node, parent);
    }
  }

  /** Walks over a tree, applying a consumer to each node. */
  public static void traverse(AstNode root, Consumer<AstNode> consumer) {
    AstVisitorImpl visitor = new AstVisitorImpl() {
      @Override public void visit(Ast.Call2 call2, @Nullable AstNode parent) {
        consumer.accept(call2);
        super.visit(call2, parent);
      }

      @Override public void visit(Ast.MatchesAdvanced matchesAdvanced,
          @Nullable AstNode parent) {
        consumer.accept(matchesAdvanced);
        super.visit(matchesAdvanced, parent);
      }

      @Override public void visit(Ast.Call1 call1, @Nullable AstNode parent) {
        consumer.accept(call1);
        super.visit(call1, parent);
      }

      @Override public void visit(Ast.Call0 call0, @Nullable AstNode parent) {
        consumer.accept(call0);
        super.visit(call0, parent);
      }

      @Override public void visit(Ast.Comparison literal,
          @Nullable AstNode parent) {
        consumer.accept(literal);
        super.visit(literal, parent);
      }

      @Override public void visit(Ast.Range range, @Nullable AstNode parent) {
        consumer.accept(range);
        super.visit(range, parent);
      }
    };
    root.accept(visitor, null);
  }

  /** Ensures that every node has a unique id. */
  public static AstNode applyId(AstNode root) {
    root.accept(new NumberingVisitor(), null);
    return root;
  }

  /** Removes node from the AST. */
  public static @Nullable AstNode removeNode(AstNode root, Integer nodeId) {
    // Difference with the TypeScript version:
    //  * Does not clone the tree first (treats it as immutable)
    //  * Only handles the case where the root has the given node id,
    //    or the desired node is a child of a logical expression (COMMA).
    if (nodeId.equals(root.id)) {
      return null;
    }
    if (root.op == Op.COMMA) {
      final Ast.Call2 call2 = (Ast.Call2) root;
      final @Nullable AstNode left2 = removeNode(call2.left, nodeId);
      final @Nullable AstNode right2 = removeNode(call2.right, nodeId);
      if (left2 == null) {
        return right2;
      }
      if (right2 == null) {
        return left2;
      }
      return ast.logicalExpression(left2, right2);
    }
    return root;
  }

  /** Converts a type to an option.
   *
   * <p>See
   * <a href="https://github.com/looker-open-source/components/blob/main/packages/filter-expressions/src/utils/option/convert_type_to_option.ts">
   * convert_type_to_option.ts</a>.
   */
  public static String convertTypeToOption(boolean is, String type) {
    return is ? type : "!" + type;
  }

  /** Converts filter types to matches (advanced).
   *
   * <p>dateFilter 'day', type 'thisRange', and type 'pastAgo' need to be in the
   * filter list, but we do not want it showing up in the advanced filter
   * options therefore it should be converted to matches (advanced).
   *
   * <p>See
   * <a href="https://github.com/looker-open-source/components/blob/main/packages/filter-expressions/src/utils/option/convert_type_to_matches_advanced_option.ts">
   * convert_type_to_matches_advanced_option.ts</a>.
   */
  public static String convertTypeToMatchesAdvancedOption(String type) {
    return type.equals("day")
        || type.equals("thisRange")
        || type.equals("pastAgo")
        || type.startsWith("before_")
        || type.startsWith("after_")
        ? "matchesAdvanced"
        : type;
  }

  /** Converts the AST to an array of FilterItems and then
   * converts each item into its expression representation. */
  public static String dateFilterToString(AstNode root, TypeFamily typeFamily) {
    return treeToString(root, node -> node.dateString(typeFamily.isDateTime()),
        node -> true);
  }

  /** Callback for a node in a traversal.
   *
   * @param <R> return type
   */
  @FunctionalInterface
  interface NodeHandler<R> {
    R apply(AstNode node, @Nullable AstNode parent);
  }

  /** Visitor that assigns a unique {@link AstNode#id} to every node in a tree.
   * Modifies the tree.  */
  private static class NumberingVisitor extends AstVisitorImpl {
    final Set<Integer> set = new HashSet<>();

    void handle(AstNode node) {
      if (node.id == null || !set.add(node.id)) {
        for (int i = set.size();; i++) {
          Integer id = i;
          if (set.add(id)) {
            node.id = id;
            break;
          }
        }
      }
    }

    @Override public void visit(Ast.Call2 call2, @Nullable AstNode parent) {
      handle(call2);
      super.visit(call2, parent);
    }

    @Override public void visit(Ast.Call1 call1, @Nullable AstNode parent) {
      handle(call1);
      super.visit(call1, parent);
    }

    @Override public void visit(Ast.Call0 call0, @Nullable AstNode parent) {
      handle(call0);
      super.visit(call0, parent);
    }

    @Override public void visit(Ast.Comparison literal,
        @Nullable AstNode parent) {
      handle(literal);
      super.visit(literal, parent);
    }

    @Override public void visit(Ast.Range range, @Nullable AstNode parent) {
      handle(range);
      super.visit(range, parent);
    }
  }
}

// End Asts.java
