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

import nl.knaw.huygens.hypercollate.collator.CollatedMatch;
import nl.knaw.huygens.hypercollate.collator.DecisionTreeNode;
import nl.knaw.huygens.hypercollate.collator.QuantumCollatedMatchList;
import nl.knaw.huygens.hypercollate.model.CollationGraph;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DecisionTreeDotBuilder {

  public static final String RED = "#f46349";
  public static final String GREEN = "#78b259";
  public static final String ORANGE = "#f48341";
  private final CollationIterationData iterationData;
  private final CollationGraph collationGraph;

  public DecisionTreeDotBuilder(CollationGraph collationGraph, String witnessSigil) {
    this.iterationData = collationGraph.getCollationIterationData(witnessSigil);
    this.collationGraph = collationGraph;
  }

  public String build() {
    StringBuilder dotBuilder = new StringBuilder()
        .append("digraph DecisionTree{\n")
        .append("  forcelabels=true\n")

        .append("  label=\"2. Decision tree for collating witness ")
        .append(iterationData.getWitnessSigil())
        .append(" against the collation graph ")
        .append(iterationData.getCollationGraphSigils())
        .append(";\n")
        .append("bold matches are chosen, strike-through matches are discarded, others are potential;\n")
        .append("arrow numbers indicate number of matches discarded since the root node.\n")
        .append("red leaf nodes indicate a dead end,\n")
        .append("orange leaf nodes indicate a sub-optimal match set,\n")
        .append("green leaf nodes indicate the optimal match set.\"\n")
        .append("  node[shape=box;style=filled]")
        .append("  labelfontname=Helvetica\n\n");
    AtomicInteger nodeCounter = new AtomicInteger(0);
    visualize(iterationData.getDecisionTree(),
        dotBuilder,
        nodeCounter,
        nodeCounter.getAndIncrement()
    );
    return dotBuilder.append("}").toString();
  }

  private void visualize(DecisionTreeNode decisionTreeNode, StringBuilder dotBuilder, AtomicInteger nodeCounter, int nodeNum) {
    QuantumCollatedMatchList quantumCollatedMatchList = decisionTreeNode.getQuantumCollatedMatchList();

    List<DecisionTreeNode> childNodes = decisionTreeNode.getChildNodes();
    List<CollatedMatch> chosenTextNodeMatches = quantumCollatedMatchList.getChosenMatches();
    List<CollatedMatch> potentialTextNodeMatches = quantumCollatedMatchList.getPotentialMatches();

    String fillColor;
    if (!childNodes.isEmpty()) {
      // node has children
      fillColor = "white";
    } else {
      //node is leafnode
      QuantumCollatedMatchList winningMatchList = decisionTreeNode.getQuantumCollatedMatchList();
      if (winningMatchList.isDetermined()) {
        fillColor = winningMatchList.getChosenMatches().equals(iterationData.getOptimalCollatedMatchList())
            ? GREEN
            : ORANGE;
      } else {
        fillColor = RED;
      }
    }
    StringBuilder matchListBuilder = new StringBuilder();
    final List<CollatedMatch> matches = iterationData.getPotentialMatches();
    for (int i = 0; i < matches.size(); i++) {
      String matchId = "m" + i;
      if (chosenTextNodeMatches.contains(matches.get(i))) {
        matchListBuilder.append("<b>").append(matchId).append("</b>");

      } else if (potentialTextNodeMatches.contains(matches.get(i))) {
        matchListBuilder.append(matchId);

      } else {
        matchListBuilder.append("<s>").append(matchId).append("</s>");
      }
      if (i < matches.size() - 1) {
        matchListBuilder.append("|");
      }

    }
    String line = String.format("n%s[label=<%s: %s>,fillcolor=\"%s\"]%n",//
        nodeNum, decisionTreeNode.getNumber(), matchListBuilder.toString(), fillColor);
    dotBuilder.append(line);

    for (DecisionTreeNode treeNode : childNodes) {
      int childNodeNum = nodeCounter.getAndIncrement();
      visualize(treeNode, dotBuilder, nodeCounter, childNodeNum);
      line = String.format("n%s->n%s[label=\"%s\"]%n", nodeNum, childNodeNum, treeNode.getCost());
      dotBuilder.append(line);
    }
  }

}
