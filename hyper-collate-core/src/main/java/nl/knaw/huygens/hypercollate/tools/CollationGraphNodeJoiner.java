package nl.knaw.huygens.hypercollate.tools;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*-
 * #%L
 * hyper-collate-core
 * =======
 * Copyright (C) 2017 Huygens ING (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.google.common.base.Preconditions;

import eu.interedition.collatex.Token;
import nl.knaw.huygens.hypercollate.model.CollationGraph;
import nl.knaw.huygens.hypercollate.model.CollationGraph.Node;
import nl.knaw.huygens.hypercollate.model.MarkedUpToken;
import nl.knaw.huygens.hypergraph.core.TraditionalEdge;

public class CollationGraphNodeJoiner {

  public static CollationGraph join(CollationGraph originalGraph) {
    CollationGraph mergedGraph = new CollationGraph(originalGraph.getSigils());
    Map<Node, Node> originalToMerged = mergeNodes(originalGraph, mergedGraph);
    copyIncomingEdges(originalGraph, originalToMerged, mergedGraph);
    return mergedGraph;
  }

  private static Map<Node, Node> mergeNodes(CollationGraph originalGraph, CollationGraph mergedGraph) {
    Map<Node, Node> originalToMerged = new HashMap<>();
    Node mergedNode = mergedGraph.getRootNode();
    Boolean isRootNode = true;
    for (Node originalNode : originalGraph.traverse()) {
      if (isRootNode) {
        isRootNode = false;
        originalToMerged.put(originalNode, mergedNode);
        continue;
      }

      if (canMergeNodes(mergedNode, originalNode, originalGraph)) {
        mergeNodeTokens(mergedNode, originalNode);

      } else {
        mergedNode = copyNode(originalNode, mergedGraph);
      }
      originalToMerged.put(originalNode, mergedNode);
    }
    return originalToMerged;
  }

  private static boolean canMergeNodes(Node mergedNode, Node originalNode, CollationGraph originalGraph) {
    Collection<TraditionalEdge> incomingEdges = originalGraph.getIncomingEdges(originalNode);
    if (incomingEdges.size() != 1) {
      return false;
    }
    if (!mergedNode.getSigils().equals(originalNode.getSigils())) {
      return false;
    }
    TraditionalEdge incomingEdge = incomingEdges.iterator().next();
    Node prevNode = originalGraph.getSource(incomingEdge);
    Boolean sigilsMatch = prevNode.getSigils().equals(mergedNode.getSigils());
    if (sigilsMatch) {
      Boolean parentXPathsMatch = true;
      for (String s : mergedNode.getSigils()) {
        Token mWitnessToken = mergedNode.getTokenForWitness(s);
        Token nWitnessToken = originalNode.getTokenForWitness(s);
        if (nWitnessToken == null) {
          // it's an endtoken, so not mergable
          parentXPathsMatch = false;
        }
        String mParentXPath = ((MarkedUpToken) mWitnessToken).getParentXPath();
        String nParentXPath = ((MarkedUpToken) nWitnessToken).getParentXPath();
        parentXPathsMatch = parentXPathsMatch && mParentXPath.equals(nParentXPath);
      }
      return parentXPathsMatch;
    }
    return false;
  }

  private static void mergeNodeTokens(Node lastNode, Node originalNode) {
    for (String s : lastNode.getSigils()) {
      MarkedUpToken tokenForWitness = (MarkedUpToken) lastNode.getTokenForWitness(s);
      MarkedUpToken tokenToMerge = (MarkedUpToken) originalNode.getTokenForWitness(s);
      tokenForWitness.setContent(tokenForWitness.getContent() + tokenToMerge.getContent())//
          .setNormalizedContent(tokenForWitness.getNormalizedContent() + tokenToMerge.getNormalizedContent());
    }
  }

  private static Node copyNode(Node originalNode, CollationGraph mergedGraph) {
    Token[] tokens = originalNode.getSigils()//
        .stream()//
        .map(originalNode::getTokenForWitness)//
        .map(CollationGraphNodeJoiner::cloneToken)//
        .collect(toList())//
        .toArray(new Token[] {});
    return mergedGraph.addNodeWithTokens(tokens);
  }

  private static Token cloneToken(Token original) {
    if (original instanceof MarkedUpToken) {
      return ((MarkedUpToken) original).clone();
    }
    throw new RuntimeException("Can't clone token of type " + original.getClass());
  }

  private static void copyIncomingEdges(CollationGraph originalGraph, Map<Node, Node> originalToMerged, CollationGraph mergedGraph) {
    Set<Node> linkedNodes = new HashSet<>();
    originalGraph.traverse().forEach(node -> {
      Node mergedNode = originalToMerged.get(node);
      if (!linkedNodes.contains(mergedNode)) {
        originalGraph.getIncomingEdges(node)//
            .forEach(e -> {
              Node oSource = originalGraph.getSource(e);
              Node mSource = originalToMerged.get(oSource);
              Preconditions.checkNotNull(mSource);
              Node oTarget = originalGraph.getTarget(e);
              Node mTarget = originalToMerged.get(oTarget);
              Preconditions.checkNotNull(mTarget);
              mergedGraph.addDirectedEdge(mSource, mTarget, e.getSigils());
            });
        linkedNodes.add(mergedNode);
      }
    });
  }

}
