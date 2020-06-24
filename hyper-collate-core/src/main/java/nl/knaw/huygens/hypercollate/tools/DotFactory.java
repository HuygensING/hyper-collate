package nl.knaw.huygens.hypercollate.tools;

/*-
 * #%L
 * hyper-collate-core
 * =======
 * Copyright (C) 2017 - 2020 Huygens ING (KNAW)
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
import nl.knaw.huygens.hypercollate.model.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class DotFactory {

  private final String whitespaceCharacter;

  public DotFactory(boolean emphasizeWhitespace) {
    this.whitespaceCharacter = emphasizeWhitespace ? "&#9251;" : "&nbsp;";
  }

  /**
   * Generates a .dot format string for visualizing a variant witness graph.
   *
   * @param graph The variant witness graph for which we are generating a dot file.
   * @return A string containing the contents of a .dot representation of the variant witness graph.
   */
  public String fromVariantWitnessGraphColored(VariantWitnessGraph graph) {
    StringBuilder dotBuilder =
        new StringBuilder("digraph VariantWitnessGraph{\n")
            .append("graph [rankdir=LR]\n")
            .append("node [style=\"filled\";fillcolor=\"white\"]\n");

    List<String> edges = new ArrayList<>();
    Set<Markup> openMarkup = new HashSet<>();
    AtomicInteger clusterCounter = new AtomicInteger();
    ColorContext colorContext = new ColorContext();
    for (TokenVertex tokenVertex : graph.vertices()) {
      List<Markup> markupListForTokenVertex = graph.getMarkupListForTokenVertex(tokenVertex);
      Set<Markup> opened = new HashSet<>();
      opened.addAll(openMarkup);

      List<Markup> markupToClose = new ArrayList<>();
      markupToClose.addAll(opened);
      markupToClose.removeAll(markupListForTokenVertex);
      markupToClose.sort(comparingInt(Markup::getDepth));
      markupToClose.forEach(m -> closeMarkup(m, dotBuilder));

      List<Markup> markupToOpen = new ArrayList<>();
      markupToOpen.addAll(markupListForTokenVertex);
      markupToOpen.removeAll(opened);
      markupToOpen.sort(comparingInt(Markup::getDepth));
      markupToOpen.forEach(
          m -> openMarkup(m, dotBuilder, clusterCounter.getAndIncrement(), colorContext));

      openMarkup.removeAll(markupToClose);
      openMarkup.addAll(markupToOpen);

      String tokenVariable = vertexVariable(tokenVertex);
      if (tokenVertex instanceof SimpleTokenVertex) {
        SimpleTokenVertex stv = (SimpleTokenVertex) tokenVertex;
        dotBuilder
            .append(tokenVariable)
            .append(" [label=<")
            .append(asLabel(stv.getContent(), whitespaceCharacter))
            .append(">]\n");
      } else {
        dotBuilder.append(tokenVariable).append(" [label=\"\";shape=doublecircle,rank=middle]\n");
      }
      tokenVertex
          .getOutgoingTokenVertexStream()
          .forEach(
              ot -> {
                String vertexVariable = vertexVariable(ot);
                edges.add(tokenVariable + "->" + vertexVariable);
              });
    }
    edges.stream().sorted().forEach(e -> dotBuilder.append(e).append("\n"));
    dotBuilder.append("}");
    return dotBuilder.toString();
  }

  private void openMarkup(
      Markup m, StringBuilder dotBuilder, int clusterNum, ColorContext colorContext) {
    String color = colorContext.colorFor(m.getTagName());
    dotBuilder
        .append("subgraph cluster_")
        .append(clusterNum)
        .append(" {\n")
        .append("label=<<i><b>")
        .append(m.getTagName())
        .append("</b></i>>\n")
        .append("graph[style=\"rounded,filled\";fillcolor=\"")
        .append(color)
        .append("\"]\n");
  }

  private void closeMarkup(Markup m, StringBuilder dotBuilder) {
    dotBuilder.append("}\n");
  }

  public String fromVariantWitnessGraphSimple(VariantWitnessGraph graph) {
    StringBuilder dotBuilder =
        new StringBuilder("digraph VariantWitnessGraph{\ngraph [rankdir=LR]\nlabelloc=b\n");

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
          dotBuilder
              .append(tokenVariable)
              .append(" [label=<")
              .append(asLabel(stv.getContent(), whitespaceCharacter))
              .append("<br/><i>")
              .append(markup)
              .append("</i>")
              .append(">]\n");
        } else {
          dotBuilder.append(tokenVariable).append(" [label=\"\";shape=doublecircle,rank=middle]\n");
        }
        tokenVertex
            .getOutgoingTokenVertexStream()
            .forEach(
                ot -> {
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

  private String vertexVariable(TokenVertex tokenVertex) {
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

  private final Comparator<TextNode> BY_NODE = Comparator.comparing(TextNode::toString);

  public String fromCollationGraph(CollationGraph collation, final boolean hideMarkup) {
    StringBuilder dotBuilder = new StringBuilder("digraph CollationGraph{\nlabelloc=b\n");
    Map<TextNode, String> nodeIdentifiers = new HashMap<>();

    List<TextNode> nodes = collation.traverseTextNodes();
    nodes.sort(BY_NODE);
    for (int i = 0; i < nodes.size(); i++) {
      TextNode node = nodes.get(i);
      String nodeId = "t" + String.format("%03d", i);
      nodeIdentifiers.put(node, nodeId);
      appendNodeLine(dotBuilder, node, nodeId, hideMarkup);
    }
    appendEdgeLines(dotBuilder, collation, nodeIdentifiers, nodes);

    dotBuilder.append("}");
    return dotBuilder.toString();
  }

  private void appendEdgeLines(
      StringBuilder dotBuilder,
      CollationGraph collation,
      Map<TextNode, String> nodeIdentifiers,
      List<TextNode> nodes) {
    Set<String> edgeLines = new TreeSet<>();
    for (TextNode node : nodes) {
      collation
          .getIncomingTextEdgeStream(node)
          .forEach(
              e -> {
                Node source = collation.getSource(e);
                Node target = collation.getTarget(e);
                String edgeLabel = e.getSigils().stream().sorted().collect(joining(","));
                String line =
                    String.format(
                        "%s->%s[label=\"%s\"]\n",
                        nodeIdentifiers.get(source), nodeIdentifiers.get(target), edgeLabel);
                edgeLines.add(line);
              });
    }
    edgeLines.forEach(dotBuilder::append);
  }

  private void appendNodeLine(
      StringBuilder dotBuilder, TextNode node, String nodeId, final boolean hideMarkup) {
    String labelString = generateNodeLabel(node, hideMarkup);
    if (labelString.isEmpty()) {
      dotBuilder.append(nodeId).append(" [label=\"\";shape=doublecircle,rank=middle]\n");

    } else {
      dotBuilder.append(nodeId).append(" [label=<").append(labelString).append(">]\n");
    }
  }

  private String generateNodeLabel(TextNode node, final boolean hideMarkup) {
    StringBuilder label = new StringBuilder();
    Map<String, String> contentLabel = new HashMap<>();
    Map<String, String> markupLabel = new HashMap<>();
    List<String> sortedSigils = node.getSigils().stream().sorted().collect(toList());
    String joinedSigils = sortedSigils.stream().collect(joining(","));

    prepare(node, contentLabel, markupLabel, sortedSigils);

    appendContent(label, contentLabel, sortedSigils, joinedSigils);
    if (!hideMarkup) {
      if (label.length() > 0) {
        label.append("<br/>");
      }
      appendMarkup(label, markupLabel, sortedSigils, joinedSigils);
    }
    return label.toString();
  }

  private void prepare(
      TextNode node,
      Map<String, String> contentLabel,
      Map<String, String> markupLabel,
      List<String> sortedSigils) {
    sortedSigils.forEach(
        s -> {
          Token token = node.getTokenForWitness(s);
          if (token != null) {
            MarkedUpToken mToken = (MarkedUpToken) token;
            String markup = mToken.getParentXPath();
            contentLabel.put(s, asLabel(mToken.getContent(), whitespaceCharacter));
            markupLabel.put(s, markup);
          }
        });
  }

  private void appendMarkup(
      StringBuilder label,
      Map<String, String> markupLabel,
      List<String> sortedSigils,
      String joinedSigils) {
    Set<String> markupLabelSet = new HashSet<>(markupLabel.values());
    if (markupLabelSet.size() == 1) {
      label
          .append(joinedSigils)
          .append(": <i>")
          .append(markupLabelSet.iterator().next())
          .append("</i>");

    } else {
      sortedSigils.forEach(
          s -> label.append(s).append(": <i>").append(markupLabel.get(s)).append("</i><br/>"));
    }
  }

  private void appendContent(
      StringBuilder label,
      Map<String, String> contentLabel,
      List<String> sortedSigils,
      String joinedSigils) {
    Set<String> contentLabelSet = new HashSet<>(contentLabel.values());
    if (contentLabelSet.size() == 1) {
      label.append(joinedSigils).append(": ").append(contentLabelSet.iterator().next());

    } else {
      String witnessLines =
          sortedSigils.stream().map(s -> s + ": " + contentLabel.get(s)).collect(joining("<br/>"));
      label.append(witnessLines);
    }
  }

  private String asLabel(String content, String whitespaceCharacter) {
    return content
        .replaceAll("&", "&amp;")
        .replaceAll("\n", "&#x21A9;<br/>")
        .replaceAll(" +", whitespaceCharacter);
  }
}
