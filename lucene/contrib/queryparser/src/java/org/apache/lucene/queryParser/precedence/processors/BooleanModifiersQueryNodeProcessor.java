package org.apache.lucene.queryParser.precedence.processors;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.queryParser.core.QueryNodeException;
import org.apache.lucene.queryParser.core.nodes.AndQueryNode;
import org.apache.lucene.queryParser.core.nodes.BooleanQueryNode;
import org.apache.lucene.queryParser.core.nodes.ModifierQueryNode;
import org.apache.lucene.queryParser.core.nodes.OrQueryNode;
import org.apache.lucene.queryParser.core.nodes.QueryNode;
import org.apache.lucene.queryParser.core.nodes.ModifierQueryNode.Modifier;
import org.apache.lucene.queryParser.core.processors.QueryNodeProcessorImpl;
import org.apache.lucene.queryParser.precedence.PrecedenceQueryParser;
import org.apache.lucene.queryParser.standard.config.DefaultOperatorAttribute;
import org.apache.lucene.queryParser.standard.config.DefaultOperatorAttribute.Operator;

/**
 * <p>
 * This processor is used to apply the correct {@link ModifierQueryNode} to {@link BooleanQueryNode}s children.
 * </p>
 * <p>
 * It walks through the query node tree looking for {@link BooleanQueryNode}s. If an {@link AndQueryNode} is found,
 * every child, which is not a {@link ModifierQueryNode} or the {@link ModifierQueryNode} 
 * is {@link Modifier#MOD_NONE}, becomes a {@link Modifier#MOD_REQ}. For any other
 * {@link BooleanQueryNode} which is not an {@link OrQueryNode}, it checks the default operator is {@link Operator#AND},
 * if it is, the same operation when an {@link AndQueryNode} is found is applied to it.
 * </p>
 * 
 * @see DefaultOperatorAttribute
 * @see PrecedenceQueryParser#setDefaultOperator
 */
public class BooleanModifiersQueryNodeProcessor extends QueryNodeProcessorImpl {

  private ArrayList<QueryNode> childrenBuffer = new ArrayList<QueryNode>();

  private Boolean usingAnd = false;

  public BooleanModifiersQueryNodeProcessor() {
    // empty constructor
  }

  @Override
  public QueryNode process(QueryNode queryTree) throws QueryNodeException {

    if (!getQueryConfigHandler().hasAttribute(DefaultOperatorAttribute.class)) {
      throw new IllegalArgumentException(
          "DefaultOperatorAttribute should be set on the QueryConfigHandler");
    }

    this.usingAnd = Operator.AND == getQueryConfigHandler().getAttribute(
        DefaultOperatorAttribute.class).getOperator();

    return super.process(queryTree);

  }

  @Override
  protected QueryNode postProcessNode(QueryNode node) throws QueryNodeException {

    if (node instanceof AndQueryNode) {
      this.childrenBuffer.clear();
      List<QueryNode> children = node.getChildren();

      for (QueryNode child : children) {
        this.childrenBuffer.add(applyModifier(child, Modifier.MOD_REQ));
      }

      node.set(this.childrenBuffer);

    } else if (this.usingAnd && node instanceof BooleanQueryNode
        && !(node instanceof OrQueryNode)) {

      this.childrenBuffer.clear();
      List<QueryNode> children = node.getChildren();

      for (QueryNode child : children) {
        this.childrenBuffer.add(applyModifier(child, Modifier.MOD_REQ));
      }

      node.set(this.childrenBuffer);

    }

    return node;

  }

  private QueryNode applyModifier(QueryNode node, Modifier mod) {

    // check if modifier is not already defined and is default
    if (!(node instanceof ModifierQueryNode)) {
      return new ModifierQueryNode(node, mod);

    } else {
      ModifierQueryNode modNode = (ModifierQueryNode) node;

      if (modNode.getModifier() == Modifier.MOD_NONE) {
        return new ModifierQueryNode(modNode.getChild(), mod);
      }

    }

    return node;

  }

  @Override
  protected QueryNode preProcessNode(QueryNode node) throws QueryNodeException {
    return node;
  }

  @Override
  protected List<QueryNode> setChildrenOrder(List<QueryNode> children)
      throws QueryNodeException {

    return children;

  }

}
