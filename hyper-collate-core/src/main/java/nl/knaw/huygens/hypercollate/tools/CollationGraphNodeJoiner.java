package nl.knaw.huygens.hypercollate.tools;

import com.google.common.base.Preconditions;
import eu.interedition.collatex.Token;
import static java.util.stream.Collectors.toList;
import nl.knaw.huygens.hypercollate.model.*;

import java.util.*;

/*-
 * #%L
 * hyper-collate-core
 * =======
 * Copyright (C) 2017 - 2018 Huygens ING (KNAW)
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

public class CollationGraphNodeJoiner {

  public static CollationGraph join(CollationGraph originalGraph) {
    CollationGraph mergedGraph = new CollationGraph(originalGraph.getSigils());
    originalGraph.getMarkupNodeStream()//
        .forEach(markupNode -> mergedGraph.addMarkupNode(markupNode.getSigil(), markupNode.getMarkup()));
    Map<TextNode, TextNode> originalToMerged = mergeNodes(originalGraph, mergedGraph);
    copyIncomingEdges(originalGraph, originalToMerged, mergedGraph);
    copyMarkupHyperEdges(originalGraph, originalToMerged, mergedGraph);
    return mergedGraph;
  }

  private static Map<TextNode, TextNode> mergeNodes(CollationGraph originalGraph, CollationGraph mergedGraph) {
    Map<TextNode, TextNode> originalToMerged = new HashMap<>();
    TextNode mergedNode = mergedGraph.getTextStartNode();
    Boolean isRootNode = true;
    for (TextNode originalNode : originalGraph.traverseTextNodes()) {
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

  private static boolean canMergeNodes(TextNode mergedNode, TextNode originalNode, CollationGraph originalGraph) {
    Collection<TextEdge> incomingEdges = originalGraph.getIncomingTextEdgeStream(originalNode).collect(toList());
    if (incomingEdges.size() != 1) {
      return false;
    }
    if (!mergedNode.getSigils().equals(originalNode.getSigils())) {
      return false;
    }
    TextEdge incomingEdge = incomingEdges.iterator().next();
    TextNode prevNode = (TextNode) originalGraph.getSource(incomingEdge);
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

  private static void mergeNodeTokens(TextNode lastNode, TextNode originalNode) {
    originalNode.getSigils().forEach(s -> lastNode.addBranchPath(s, originalNode.getBranchPath(s)));
    for (String s : lastNode.getSigils()) {
      MarkedUpToken tokenForWitness = (MarkedUpToken) lastNode.getTokenForWitness(s);
      MarkedUpToken tokenToMerge = (MarkedUpToken) originalNode.getTokenForWitness(s);
      tokenForWitness.setContent(tokenForWitness.getContent() + tokenToMerge.getContent())//
          .setNormalizedContent(tokenForWitness.getNormalizedContent() + tokenToMerge.getNormalizedContent());
    }
  }

  private static TextNode copyNode(TextNode originalNode, CollationGraph mergedGraph) {
    Token[] tokens = originalNode.getSigils()//
        .stream()//
        .map(originalNode::getTokenForWitness)//
        .map(CollationGraphNodeJoiner::cloneToken)//
        .collect(toList())//
        .toArray(new Token[]{});
    TextNode newNode = mergedGraph.addTextNodeWithTokens(tokens);
    originalNode.getSigils().forEach(s -> newNode.addBranchPath(s, originalNode.getBranchPath(s)));
    return newNode;
  }

  private static Token cloneToken(Token original) {
    if (original instanceof MarkedUpToken) {
      return ((MarkedUpToken) original).clone();
    }
    throw new RuntimeException("Can't clone token of type " + original.getClass());
  }

  private static void copyIncomingEdges(CollationGraph originalGraph, Map<TextNode, TextNode> originalToMerged, CollationGraph mergedGraph) {
    Set<Node> linkedNodes = new HashSet<>();
    originalGraph.traverseTextNodes().forEach(node -> {
      Node mergedNode = originalToMerged.get(node);
      if (!linkedNodes.contains(mergedNode)) {
        originalGraph.getIncomingTextEdgeStream(node)//
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

  private static void copyMarkupHyperEdges(CollationGraph originalGraph, Map<TextNode, TextNode> originalToMerged, CollationGraph mergedGraph) {
    originalGraph.getMarkupStream().forEach(m -> {
      MarkupNode mergedMarkupNode = mergedGraph.getMarkupNode(m);
      originalGraph.getTextNodeStreamForMarkup(m)//
          .map(originalToMerged::get)//
          .distinct()//
          .forEach(mergedTextNode -> mergedGraph.linkMarkupToText(mergedMarkupNode, mergedTextNode));
    });
  }

}
