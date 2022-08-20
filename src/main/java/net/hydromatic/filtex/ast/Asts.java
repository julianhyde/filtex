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

import com.google.common.collect.ImmutableList;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.hydromatic.filtex.ast.AstBuilder.ast;

/** Utilities for AST nodes. */
public class Asts {
  private Asts() {}

  /**
   * Converts a FilterAST to a list of FilterASTNodes using in order traversal
   * (left, node, right)
   *
   * <pre>
   *    (root(1))      ->  [0,1,2,3,4]
   *    /      \
   * left(0)   right(3)
   *           /     \
   *      left(2)     right(4)
   * </pre>
   *
   * <p>See
   * <a href="https://github.com/looker-open-source/components/blob/main/packages/filter-expressions/src/utils/tree/tree_to_list.ts">
   * tree_to_list.ts</a>
   */
  public static List<Model> treeToList(AstNode root) {
    final List<Model> orItems = new ArrayList<>();
    final List<Model> andItems = new ArrayList<>();
    final AstNode.Visitor visitor =
        new AstNode.VisitorImpl() {
          @Override public void infix(Ast.Call2 call2, @Nullable AstNode parent) {
            if (call2.op != Op.COMMA) {
              Model model = call2.model();
              (model.is ? orItems : andItems).add(model);
            }
          }

          @Override public void visit(Ast.Comparison literal,
              @Nullable AstNode parent) {
            final Model model = literal.model();
            (model.is ? orItems : andItems).add(model);
          }

          @Override public void visit(Ast.Range range,
              @Nullable AstNode parent) {
            List<Model> list = range.is ? orItems : andItems;
            if (range.left != null) {
              Op op = range.op.containsLowerBound() ? Op.GE : Op.GT;
              AstNode comparison = ast.comparison(true, op, range.left);
              list.add(comparison.model());
            }
            if (range.right != null) {
              Op op = range.op.containsUpperBound() ? Op.LE : Op.LT;
              AstNode comparison = ast.comparison(true, op, range.right);
              list.add(comparison.model());
              list.add(model(range.right));
            }
          }

          private Model model(BigDecimal number) {
            return new Model(true, "number",
                ImmutableList.of(number.toString()), null, null, null);
          }
        };
    root.accept(visitor, null);
    return ImmutableList.<Model>builder()
        .addAll(orItems)
        .addAll(andItems)
        .build();
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

  /** Converts a type to an option.
   *
   * <p>See
   * <a href="https://github.com/looker-open-source/components/blob/main/packages/filter-expressions/src/utils/option/cpmvert+type_to_option.ts">
   * convert_type_to_option.ts</a>.
   */
  public static String convertTypeToOption(boolean is, String type) {
    return is ? type : "!" + type;
  }

  /** Callback for a node in a traversal.
   *
   * @param <R> return type
   */
  @FunctionalInterface
  interface NodeHandler<R> {
    R apply(AstNode node, @Nullable AstNode parent);
  }

  /** Model of an item in a filter expression. */
  public static class Model {
    /** False if negated, true if not negated. */
    public final boolean is;
    /** Returns the FilterModel type. For example, "," for "OR". */
    public final String type;
    public final @Nullable Object value;
    public final @Nullable String bounds;
    public final @Nullable String low;
    public final @Nullable String high;

    public Model(boolean is, String type, @Nullable Object value,
        String bounds, String low, String high) {
      this.is = is;
      this.type = type;
      this.value = value;
      this.bounds = bounds;
      this.low = low;
      this.high = high;
    }

    @Override public String toString() {
      StringBuilder b = new StringBuilder();
      b.append("{is ").append(is);
      if (value != null) {
        b.append(", value ").append(value);
      }
      if (bounds != null) {
        b.append(", bounds ").append(bounds);
      }
      if (low != null) {
        b.append(", low ").append(low);
      }
      if (high != null) {
        b.append(", high ").append(high);
      }
      return b.append("}").toString();
    }

    @Override public int hashCode() {
      return Objects.hash(is, type, value, bounds, low, high);
    }

    @Override public boolean equals(Object o) {
      return o == this
          || o instanceof Model
          && is == ((Model) o).is
          && type.equals(((Model) o).type)
          && Objects.equals(value, ((Model) o).value)
          && Objects.equals(bounds, ((Model) o).bounds)
          && Objects.equals(low, ((Model) o).low)
          && Objects.equals(high, ((Model) o).high);
    }
  }
}

// End Asts.java
