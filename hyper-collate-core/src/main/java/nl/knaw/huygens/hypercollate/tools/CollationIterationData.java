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

import java.util.ArrayList;
import java.util.List;

public class CollationIterationData {
  private DecisionTreeNode decisionTree;
  private List<String> collationGraphSigils = new ArrayList<>();
  private String witnessSigil;
  private List<CollatedMatch> potentialMatches = new ArrayList<>();

  public CollationIterationData setDecisionTree(final DecisionTreeNode decisionTree) {
    this.decisionTree = decisionTree;
    return this;
  }

  public DecisionTreeNode getDecisionTree() {
    return decisionTree;
  }

  public CollationIterationData setCollationGraphSigils(final List<String> collationGraphSigils) {
    this.collationGraphSigils.addAll(collationGraphSigils);
    return this;
  }

  public List<String> getCollationGraphSigils() {
    return collationGraphSigils;
  }

  public CollationIterationData setWitnessSigil(final String witnessSigil) {
    this.witnessSigil = witnessSigil;
    return this;
  }

  public String getWitnessSigil() {
    return witnessSigil;
  }

  public CollationIterationData setPotentialMatches(final List<CollatedMatch> potentialMatches) {
    this.potentialMatches.addAll(potentialMatches);
    return this;
  }

  public List<CollatedMatch> getPotentialMatches() {
    return potentialMatches;
  }

  //    String iterationId = collationGraph.getSigils().stream().collect(joining()) + "+" + witnessSigil;

}
