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
import nl.knaw.huygens.hypercollate.model.CollationGraph;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public class PotentialMatchesGraphDotBuilder {

  private final CollationGraph collationGraph;
  private final CollationIterationData iterationData;

  public PotentialMatchesGraphDotBuilder(CollationGraph collationGraph, String witnessSigil) {
    this.iterationData = collationGraph.getCollationIterationData(witnessSigil);
    this.collationGraph = collationGraph;
  }

  public String build() {
    StringBuilder dotBuilder = new StringBuilder()
        .append("digraph PotentialMatchesGraph{\n")
        .append("  rankdir=LR\n")
        .append("  label=\"potential matches (red) for collating witness ")
        .append(iterationData.getWitnessSigil())
        .append(" (blue) against the collation graph (black)\"\n")
        .append("  forcelabels=true\n")
        .append("  labelfontname=Helvetica\n")
        .append("  begin [label=\"\";shape=doublecircle,rank=middle]\n")
        .append("  end [label=\"\";shape=doublecircle,rank=middle]\n");

    Map<Object, String> nodeNames = new HashMap<>();

    // collation graph nodes
//    collationGraph.traverseTextNodes().stream()
    String node = "t002";
    String label = "A: The&#9251;";
    dotBuilder
        .append(format("    %s [label=<%s>]\n", node, label))
        .append("    t003 [label=<A: dog's&#9251;>]\n")
        .append("    t004 [label=<A: big&#9251;>]\n")
        .append("    t005 [label=<A: eyes>]\n")
        .append("    t007 [label=<A: brown>]\n")
        .append("    t006 [label=<A: .>]\n");

    // collation graph edges
    dotBuilder
        .append("    begin->t002[label=\"A\"]\n")
        .append("    t002->t003[label=\"A\"]\n")
        .append("    t003->t004[label=\"A\"]\n")
        .append("    t004->t005[label=\"A\"]\n")
        .append("    t005->t007[label=\"A\"]\n")
        .append("    t007->t006[label=\"A\"]\n")
        .append("    t006->end[label=\"A\"]\n");

    // witness graph nodes
    dotBuilder
        .append("    B_000 [fontcolor=blue;color=blue;label=<The&#9251;>]\n")
        .append("    B_001 [fontcolor=blue;color=blue;label=<dog's&#9251;>]\n")
        .append("    B_002 [fontcolor=blue;color=blue;label=<big&#9251;>]\n")
        .append("    B_005 [fontcolor=blue;color=blue;label=<brown&#9251;>]\n")
        .append("    B_003 [fontcolor=blue;color=blue;label=<black&#9251;>]\n")
        .append("    B_006 [fontcolor=blue;color=blue;label=<eyes>]\n")
        .append("    B_004 [fontcolor=blue;color=blue;label=<ears>]\n")
        .append("    B_007 [fontcolor=blue;color=blue;label=<.>]\n");

    // witness graph edges
    dotBuilder
        .append("    begin->B_000[fontcolor=blue;color=blue;label=\"B\"]\n")
        .append("    B_000->B_001[fontcolor=blue;color=blue;label=\"B\"]\n")
        .append("    B_001->B_002[fontcolor=blue;color=blue;label=\"B\"]\n")
        .append("    B_001->B_005[fontcolor=blue;color=blue;label=\"B\"]\n")
        .append("    B_002->B_003[fontcolor=blue;color=blue;label=\"B\"]\n")
        .append("    B_003->B_004[fontcolor=blue;color=blue;label=\"B\"]\n")
        .append("    B_004->B_007[fontcolor=blue;color=blue;label=\"B\"]\n")
        .append("    B_005->B_006[fontcolor=blue;color=blue;label=\"B\"]\n")
        .append("    B_006->B_007[fontcolor=blue;color=blue;label=\"B\"]\n")
        .append("    B_007->end[fontcolor=blue;color=blue;label=\"B\"]\n");

    // potential matches edges
    dotBuilder
        .append("    t002->B_000 [fontcolor=red;color=red;xlabel=\"m1 \";dir=none;style=dashed;constraint=false;bgcolor=white]\n")
        .append("    t003->B_001 [fontcolor=red;color=red;xlabel=\"m2 \";dir=none;style=dashed;constraint=false;]\n")
        .append("    t004->B_002 [fontcolor=red;color=red;xlabel=\"m3 \";dir=none;style=dashed;constraint=false;bgcolor=white]\n")
        .append("    t005->B_006 [fontcolor=red;color=red;xlabel=\"m4 \";dir=none;style=dashed;constraint=false;]\n")
        .append("    t006->B_007 [fontcolor=red;color=red;xlabel=\"m5 \";dir=none;style=dashed;constraint=false;]\n")
        .append("    t007->B_005 [fontcolor=red;color=red;xlabel=\"m6 \";dir=none;style=dashed;constraint=false;]\n");

    return dotBuilder.append("}").toString();
  }

}
