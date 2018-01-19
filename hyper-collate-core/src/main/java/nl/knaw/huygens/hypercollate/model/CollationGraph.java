package nl.knaw.huygens.hypercollate.model;

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

import eu.interedition.collatex.Token;
import static java.util.stream.Collectors.toList;
import nl.knaw.huygens.hypergraph.core.Hypergraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

public class CollationGraph extends Hypergraph<Node, Edge> {
  private static final Logger LOG = LoggerFactory.getLogger(CollationGraph.class);
  private final List<String> sigils;
  private TextDelimiterNode textStartNode = new TextDelimiterNode();
  private TextDelimiterNode textEndNode = new TextDelimiterNode();
  Map<Markup, MarkupNode> markupNodeIndex = new HashMap<>();

  public CollationGraph() {
    this(new ArrayList<>());
  }

  public CollationGraph(List<String> sigils) {
    super(GraphType.ORDERED);
    this.sigils = sigils;
    textStartNode.setSigils(sigils);
    textEndNode.setSigils(sigils);
  }

  public TextNode addTextNodeWithTokens(Token... tokens) {
    TextNode newNode = new TextNode(tokens);
    addNode(newNode, TextNode.LABEL);
    return newNode;
  }

  public MarkupNode addMarkupNode(Markup markup) {
    MarkupNode newNode = new MarkupNode(markup);
    addNode(newNode, MarkupNode.LABEL);
    markupNodeIndex.put(markup, newNode);
    return newNode;
  }

  public void linkMarkupToText(MarkupNode markupNode, TextNode textNode) {
    List<MarkupHyperEdge> markupHyperEdges = getOutgoingEdges(markupNode).stream()
        .filter(MarkupHyperEdge.class::isInstance)
        .map(MarkupHyperEdge.class::cast)
        .collect(toList());
    if (markupHyperEdges.isEmpty()) {
      MarkupHyperEdge newEdge = new MarkupHyperEdge();
      addDirectedHyperEdge(newEdge, MarkupHyperEdge.LABEL, markupNode, textNode);
    } else {
      if (markupHyperEdges.size() != 1) {
        throw new RuntimeException("MarkupNode " + markupNode + " should have exactly 1 MarkupHyperEdge, but has " + markupHyperEdges.size());
      }
      MarkupHyperEdge edge = markupHyperEdges.get(0);
      addTargetsToHyperEdge(edge, textNode);
    }
  }

  public TextDelimiterNode getTextStartNode() {
    return textStartNode;
  }

  public TextDelimiterNode getTextEndNode() {
    return textEndNode;
  }

  public boolean isEmpty() {
    return sigils.isEmpty();
  }

  public List<String> getSigils() {
    return sigils;
  }

  public TextNode getTarget(TextEdge edge) {
    Collection<Node> nodes = getTargets(edge);
    if (nodes.size() != 1) {
      throw new RuntimeException("trouble!");
    }
    return (TextNode) nodes.iterator().next();
  }

  public void addDirectedEdge(Node source, Node target, Set<String> sigils) {
    TextEdge edge = new TextEdge(sigils);
    super.addDirectedHyperEdge(edge, TextEdge.LABEL, source, target);
  }

  public Stream<TextEdge> getOutgoingTextEdgeStream(Node source) {
    return this.getOutgoingEdges(source)
        .stream()//
        .filter(TextEdge.class::isInstance)
        .map(TextEdge.class::cast);
  }

  public List<TextNode> traverseTextNodes() {
    Set<Node> visitedNodes = new HashSet<>();
    Stack<Node> nodesToVisit = new Stack<>();
    nodesToVisit.add(textStartNode);
    List<TextNode> result = new ArrayList<>();
    while (!nodesToVisit.isEmpty()) {
      Node pop = nodesToVisit.pop();
      if (!visitedNodes.contains(pop)) {
        if (pop instanceof TextNode) {
          result.add((TextNode) pop);
        }
        visitedNodes.add(pop);
        getOutgoingTextEdgeStream(pop).forEach(e -> {
          Node target = this.getTarget(e);
          if (target == null) {
            throw new RuntimeException("edge target is null for edge " + pop + "->");
          }
          nodesToVisit.add(target);
        });
      } else {
        LOG.debug("revisiting node {}", pop);
      }
    }
    return result;
  }

  public Stream<TextEdge> getIncomingTextEdgeStream(TextNode node) {
    return getIncomingEdges(node).stream()
        .filter(TextEdge.class::isInstance)
        .map(TextEdge.class::cast);
  }
}
