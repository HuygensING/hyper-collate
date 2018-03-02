package nl.knaw.huygens.hypercollate.tools;

/*-
 * #%L
 * HyperCollate core
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

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import eu.interedition.collatex.Token;
import nl.knaw.huygens.hypercollate.model.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Iterables.cycle;
import static java.lang.String.format;
import static java.util.Collections.disjoint;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class PotentialMatchesGraphDotBuilder {

  private static final String COLLATIONGRAPH_NODE_TEMPLATE = "  %s [label=<%s>]\n";
  private static final String COLLATIONGRAPH_EDGE_TEMPLATE = "  %s->%s [label=\"%s\"]\n";
  private static final String WITNESS_NODE_TEMPLATE = "  %s [fontcolor=blue;color=blue;label=<%s>]\n";
  private static final String WITNESS_EDGE_TEMPLATE = "  %s->%s [fontcolor=blue;color=blue;label=\"%s\"]\n";
  private static final String MATCH_EDGE_TEMPLATE = "  %s->%s [fontcolor=red;color=red;xlabel=\"%s \";dir=none;style=%s;constraint=false;bgcolor=white]\n";
  private static final String SAME_RANK_TEMPLATE = "  {rank=same;%s %s}\n";

  private static final Iterator<String> STYLE_CYCLE = cycle(new String[]{"dashed", "solid"}).iterator();

  private final CollationGraph collationGraph;
  private final CollationIterationData iterationData;
  private final AtomicInteger nodeCounter = new AtomicInteger();

  public PotentialMatchesGraphDotBuilder(CollationGraph collationGraph, String witnessSigil) {
    this.iterationData = collationGraph.getCollationIterationData(witnessSigil);
    this.collationGraph = collationGraph;
  }

  public String build() {
    List<String> collationGraphSigils = iterationData.getCollationGraphSigils();
    String witnessSigil = iterationData.getWitnessSigil();

    StringBuilder dotBuilder = new StringBuilder()
        .append("digraph PotentialMatchesGraph{\n")
        .append("  rankdir=LR\n")
        .append("  label=\"potential matches (red) for collating witness ")
        .append(witnessSigil)
        .append(" (blue) against the collation graph ")
        .append(collationGraphSigils)
        .append(" (black)\"\n")
        .append("  forcelabels=true\n")
        .append("  labelfontname=Helvetica\n\n");

    Map<TextNode, String> collationGraphNodeNames = new HashMap<>();
    Map<TextNode, String> witnessNodeNames = new HashMap<>();

    List<TextNode> allTextNodes = collationGraph.traverseTextNodes();
    List<TextNode> collationGraphTextNodes = allTextNodes
        .stream()
        .filter(tn -> hasAnyRelevantSigils(tn, collationGraphSigils))
        .collect(toList());

    // collation graph nodes
    for (TextNode tn1 : collationGraphTextNodes) {
      String node = nextNodeVariable();
      collationGraphNodeNames.put(tn1, node);

      Multimap<String, String> labelMap = LinkedHashMultimap.create();
      collationGraphSigils.forEach(s -> {
        String content = ((MarkedUpToken) tn1.getTokenForWitness(s)).getContent();
        labelMap.put(content, s);
      });
      String label = labelMap.asMap()
          .entrySet().stream()
          .map(this::labelLine)
          .collect(joining("\n"));

      dotBuilder
          .append(format(COLLATIONGRAPH_NODE_TEMPLATE, node, label));
    }
    dotBuilder.append("\n");

    // collation graph edges
    Set<String> edgeLines = new TreeSet<>();
    for (TextNode collationGraphTextNode : collationGraphTextNodes) {
      collationGraph.getIncomingTextEdgeStream(collationGraphTextNode)//
          .filter(e -> hasAnyRelevantSigils(e, collationGraphSigils))
          .forEach(e -> {
            Node source = collationGraph.getSource(e);
            Node target = collationGraph.getTarget(e);
            if (collationGraphNodeNames.containsKey(source) && collationGraphNodeNames.containsKey(target)) {
              String edgeLabel = e.getSigils().stream()
                  .filter(collationGraphSigils::contains)
                  .sorted()
                  .collect(joining(","));
              String line = format(COLLATIONGRAPH_EDGE_TEMPLATE,//
                  collationGraphNodeNames.get(source), collationGraphNodeNames.get(target), edgeLabel);
              edgeLines.add(line);
            }
          });
    }

    List<TextNode> witnessTextNodes = allTextNodes
        .stream()
        .filter(tn -> tn.getSigils().contains(witnessSigil))
        .collect(toList());

    // witness graph nodes
    for (TextNode witnessTextNode : witnessTextNodes) {
      String node = nextNodeVariable();
      witnessNodeNames.put(witnessTextNode, node);

      String label = ((MarkedUpToken) witnessTextNode.getTokenForWitness(witnessSigil)).getContent();
      dotBuilder.append(format(WITNESS_NODE_TEMPLATE, node, label));
    }

    dotBuilder.append("\n");

    // witness graph edges
    for (TextNode node : witnessTextNodes) {
      collationGraph.getIncomingTextEdgeStream(node)//
          .filter(e -> e.getSigils().contains(witnessSigil))
          .forEach(e -> {
            Node source = collationGraph.getSource(e);
            Node target = collationGraph.getTarget(e);
            if (witnessNodeNames.containsKey(source) && witnessNodeNames.containsKey(target)) {
              String line = format(WITNESS_EDGE_TEMPLATE,//
                  witnessNodeNames.get(source), witnessNodeNames.get(target), witnessSigil);
              edgeLines.add(line);
            }
          });
    }
    edgeLines.forEach(dotBuilder::append);
    dotBuilder.append("\n");

    Map<Token, TextNode> collationGraphNodeForWitnessToken = new HashMap<>();
    witnessTextNodes.forEach(tn -> {
      Token token = tn.getTokenForWitness(witnessSigil);
      collationGraphNodeForWitnessToken.put(token, tn);
    });

    // potential matches edges
    AtomicInteger matchCounter = new AtomicInteger();
    iterationData.getPotentialMatches().forEach(m -> {
      TextNode collatedNode = m.getCollatedNode();
      String cNode = collationGraphNodeNames.get(collatedNode);
      if (cNode != null) {
        Token wToken = m.getWitnessVertex().getToken();
        TextNode textNode = collationGraphNodeForWitnessToken.get(wToken);
        String wNode = witnessNodeNames.get(textNode);
        String label = format("m%d", matchCounter.getAndIncrement());

        dotBuilder.append(format(MATCH_EDGE_TEMPLATE, cNode, wNode, label, STYLE_CYCLE.next()));
        if (collatedNode.equals(textNode)) {
          dotBuilder.append(format(SAME_RANK_TEMPLATE, cNode, wNode));
        }
      }
    });

    return dotBuilder.append("}").toString();
  }

  private String nextNodeVariable() {
    return format("n%05d", nodeCounter.getAndIncrement());
  }

  private boolean hasAnyRelevantSigils(final TextNode tn, final List<String> collationGraphSigils) {
    return !disjoint(tn.getSigils(), collationGraphSigils);
  }

  private boolean hasAnyRelevantSigils(final TextEdge e, final List<String> collationGraphSigils) {
    return !disjoint(e.getSigils(), collationGraphSigils);
  }

  private String labelLine(final Map.Entry<String, Collection<String>> e) {
    String sigils = e.getValue().stream().sorted().collect(joining(","));
    String content = e.getKey();
    return format("%s:%s", sigils, content);
  }

}
