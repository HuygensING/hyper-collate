package nl.knaw.huygens.hypercollate.tools;

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

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.interedition.collatex.Token;
import nl.knaw.huygens.hypercollate.model.CollationGraph;
import nl.knaw.huygens.hypercollate.model.CollationGraph.Node;
import nl.knaw.huygens.hypercollate.model.MarkedUpToken;
import nl.knaw.huygens.hypergraph.core.TraditionalEdge;

public class CollationGraphNodeMerger {

  public static CollationGraph merge(CollationGraph originalGraph) {
    CollationGraph mergedGraph = new CollationGraph();
    Map<Node, Node> originalToMerged = mergeNodes(originalGraph, mergedGraph);
    copyIncomingEdges(originalGraph, originalToMerged, mergedGraph);
    return mergedGraph;
  }

  private static Map<Node, Node> mergeNodes(CollationGraph originalGraph, CollationGraph mergedGraph) {
    Map<Node, Node> originalToMerged = new HashMap<>();
    Node lastNode = mergedGraph.getRootNode();
    Boolean isRootNode = true;
    for (Node originalNode : originalGraph.traverse()) {
      if (isRootNode) {
        isRootNode = false;
        originalToMerged.put(originalNode, lastNode);
        continue;
      }

      if (canMergeNodes(lastNode, originalNode, originalGraph)) {
        mergeNodeTokens(lastNode, originalNode);

      } else {
        lastNode = copyNode(originalNode, mergedGraph, originalToMerged);
      }
    }
    return originalToMerged;
  }

  private static Node copyNode(Node originalNode, CollationGraph mergedGraph, Map<Node, Node> originalToMerged) {
    Node lastNode;
    Token[] tokens = originalNode.getSigils()//
        .stream()//
        .map(originalNode::getTokenForWitness)//
        .collect(toList())//
        .toArray(new Token[] {});
    Node mergedNode = mergedGraph.addNodeWithTokens(tokens);
    originalToMerged.put(originalNode, mergedNode);
    lastNode = mergedNode;
    return lastNode;
  }

  private static void mergeNodeTokens(Node lastNode, Node originalNode) {
    for (String s : lastNode.getSigils()) {
      MarkedUpToken tokenForWitness = (MarkedUpToken) lastNode.getTokenForWitness(s);
      MarkedUpToken tokenToMerge = (MarkedUpToken) originalNode.getTokenForWitness(s);
      tokenForWitness.setContent(tokenForWitness.getContent() + tokenToMerge.getContent())//
          .setNormalizedContent(tokenForWitness.getNormalizedContent() + tokenToMerge.getNormalizedContent());
    }
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
              Node oTarget = originalGraph.getTarget(e);
              Node mTarget = originalToMerged.get(oTarget);
              mergedGraph.addDirectedEdge(mSource, mTarget, e.getSigils());
            });
        linkedNodes.add(mergedNode);
      }
    });
  }

  private static boolean canMergeNodes(Node mergedNode, Node originalNode, CollationGraph originalGraph) {
    Collection<TraditionalEdge> outgoingEdges = originalGraph.getOutgoingEdges(originalNode);
    if (outgoingEdges.size() != 1) {
      return false;
    }

    TraditionalEdge outGoingEdge = outgoingEdges.iterator().next();
    Node nextNode = originalGraph.getTarget(outGoingEdge);
    Boolean sigilsMatch = nextNode.getSigils().equals(mergedNode.getSigils());
    if (sigilsMatch) {
      for (String s : mergedNode.getSigils()) {
        Token mWitnessToken = mergedNode.getTokenForWitness(s);
        Token nWitnessToken = nextNode.getTokenForWitness(s);
        if (nWitnessToken == null) {
          // it's an endtoken, so not mergable
          return false;
        }
        String mParentXPath = ((MarkedUpToken) mWitnessToken).getParentXPath();
        String nParentXPath = ((MarkedUpToken) nWitnessToken).getParentXPath();
        return mParentXPath.equals(nParentXPath);
      }
    }
    return false;
  }

}
