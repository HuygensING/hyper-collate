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

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import eu.interedition.collatex.Token;
import nl.knaw.huygens.hypercollate.model.CollationGraph;
import nl.knaw.huygens.hypercollate.model.CollationGraph.Node;
import nl.knaw.huygens.hypercollate.model.EndTokenVertex;
import nl.knaw.huygens.hypercollate.model.MarkedUpToken;
import nl.knaw.huygens.hypercollate.model.SimpleTokenVertex;
import nl.knaw.huygens.hypercollate.model.StartTokenVertex;
import nl.knaw.huygens.hypercollate.model.TokenVertex;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;

public class DotFactory {

  /**
   * Generates a .dot format string for visualizing a variant witness graph.
   *
   * @param graph
   *          The variant witness graph for which we are generating a dot file.
   * @return A string containing the contents of a .dot representation of the
   *         variant witness graph.
   */
  public static String fromVariantWitnessGraph(VariantWitnessGraph graph) {
    StringBuilder dotBuilder = new StringBuilder("digraph VariantWitnessGraph{\ngraph [rankdir=LR]\nlabelloc=b\n");

    List<String> edges = new ArrayList<>();
    Deque<TokenVertex> nextTokens = new LinkedList<>();
    nextTokens.add(graph.getStartTokenVertex());
    Set<TokenVertex> verticesDone = new HashSet<>();
    while (!nextTokens.isEmpty()) {
      TokenVertex tokenVertex = nextTokens.pop();
      if (!verticesDone.contains(tokenVertex)) {
        String tokenVariable = vertexVariable(tokenVertex);
        if (tokenVertex instanceof SimpleTokenVertex) {
          SimpleTokenVertex stv = (SimpleTokenVertex) tokenVertex;
          String markup = graph.getSigil() + ": " + stv.getParentXPath();
          dotBuilder.append(tokenVariable)//
              .append(" [label=<")//
              .append(asLabel(stv.getContent()))//
              .append("<br/><i>")//
              .append(markup)//
              .append("</i>")//
              .append(">]\n");
        } else {
          dotBuilder.append(tokenVariable)//
              .append(" [label=\"\";shape=doublecircle,rank=middle]\n");
        }
        tokenVertex.getOutgoingTokenVertexStream().forEach(ot -> {
          String vertexVariable = vertexVariable(ot);
          edges.add(tokenVariable + "->" + vertexVariable);
          nextTokens.add(ot);
        });
        verticesDone.add(tokenVertex);
      }
    }
    edges.stream().sorted().forEach(e -> dotBuilder.append(e).append("\n"));
    dotBuilder.append("}");
    return dotBuilder.toString();
  }

  private static String vertexVariable(TokenVertex tokenVertex) {
    if (tokenVertex instanceof SimpleTokenVertex) {
      MarkedUpToken token = (MarkedUpToken) tokenVertex.getToken();
      return token.getWitness().getSigil() + "_" + String.format("%03d", token.getIndexNumber());
    }
    if (tokenVertex instanceof StartTokenVertex) {
      return "begin";
    }
    if (tokenVertex instanceof EndTokenVertex) {
      return "end";
    }
    return null;
  }

  private static final Comparator<Node> BY_NODE = Comparator.comparing(Node::toString);

  public static String fromCollationGraph(CollationGraph collation) {
    StringBuilder dotBuilder = new StringBuilder("digraph CollationGraph{\nlabelloc=b\n");
    Map<Node, String> nodeIdentifiers = new HashMap<>();

    List<Node> nodes = collation.traverse();
    nodes.sort(BY_NODE);
    for (int i = 0; i < nodes.size(); i++) {
      Node node = nodes.get(i);
      String nodeId = "t" + String.format("%03d", i);
      nodeIdentifiers.put(node, nodeId);
      appendNodeLine(dotBuilder, node, nodeId);
    }
    appendEdgeLines(dotBuilder, collation, nodeIdentifiers, nodes);

    dotBuilder.append("}");
    return dotBuilder.toString();
  }

  private static void appendEdgeLines(StringBuilder dotBuilder, CollationGraph collation, Map<Node, String> nodeIdentifiers, List<Node> nodes) {
    Set<String> edgeLines = new TreeSet<>();
    for (Node node : nodes) {
      collation.getIncomingEdges(node)//
          .forEach(e -> {
            Node source = collation.getSource(e);
            Node target = collation.getTarget(e);
            String edgeLabel = e.getSigils().stream().sorted().collect(joining(","));
            String line = nodeIdentifiers.get(source) + "->" + nodeIdentifiers.get(target) + "[label=\"" + edgeLabel + "\"]\n";
            edgeLines.add(line);
          });
    }
    edgeLines.forEach(dotBuilder::append);
  }

  private static void appendNodeLine(StringBuilder dotBuilder, Node node, String nodeId) {
    String labelString = generateNodeLabel(node);
    if (labelString.isEmpty()) {
      dotBuilder.append(nodeId)//
          .append(" [label=\"\";shape=doublecircle,rank=middle]\n");

    } else {
      dotBuilder.append(nodeId)//
          .append(" [label=<")//
          .append(labelString)//
          .append(">]\n");
    }
  }

  private static String generateNodeLabel(Node node) {
    StringBuilder label = new StringBuilder();
    Map<String, String> contentLabel = new HashMap<>();
    Map<String, String> markupLabel = new HashMap<>();
    List<String> sortedSigils = node.getSigils().stream().sorted().collect(toList());
    String joinedSigils = sortedSigils.stream().collect(joining(","));

    prepare(node, contentLabel, markupLabel, sortedSigils);

    appendContent(label, contentLabel, sortedSigils, joinedSigils);
    appendMarkup(label, markupLabel, sortedSigils, joinedSigils);

    return label.toString();
  }

  private static void prepare(Node node, Map<String, String> contentLabel, Map<String, String> markupLabel, List<String> sortedSigils) {
    sortedSigils.forEach(s -> {
      Token token = node.getTokenForWitness(s);
      if (token != null) {
        MarkedUpToken mToken = (MarkedUpToken) token;
        String markup = mToken.getParentXPath();
        contentLabel.put(s, asLabel(mToken.getContent()));
        markupLabel.put(s, markup);
      }
    });
  }

  private static void appendMarkup(StringBuilder label, Map<String, String> markupLabel, List<String> sortedSigils, String joinedSigils) {
    Set<String> markupLabelSet = new HashSet<>(markupLabel.values());
    if (markupLabelSet.size() == 1) {
      label.append(joinedSigils)//
          .append(": <i>")//
          .append(markupLabelSet.iterator().next())//
          .append("</i>");

    } else {
      sortedSigils.forEach(s -> label.append(s)//
          .append(": <i>")//
          .append(markupLabel.get(s))//
          .append("</i><br/>"));
    }
  }

  private static void appendContent(StringBuilder label, Map<String, String> contentLabel, List<String> sortedSigils, String joinedSigils) {
    Set<String> contentLabelSet = new HashSet<>(contentLabel.values());
    if (contentLabelSet.size() == 1) {
      label.append(joinedSigils)//
          .append(": ")//
          .append(contentLabelSet.iterator().next())//
          .append("<br/>");

    } else {
      sortedSigils.forEach(s -> label.append(s)//
          .append(": ")//
          .append(contentLabel.get(s))//
          .append("<br/>"));
    }
  }

  private static String asLabel(String content) {
    return content.replaceAll("\n", "&#x21A9;<br/>")//
        .replaceAll(" +", "&#9251;");
  }

}
