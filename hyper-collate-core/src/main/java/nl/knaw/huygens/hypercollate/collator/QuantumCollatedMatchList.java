package nl.knaw.huygens.hypercollate.collator;

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

import nl.knaw.huygens.hypercollate.model.TextNode;
import nl.knaw.huygens.hypercollate.model.TokenVertex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;

public class QuantumCollatedMatchList {

  private final List<CollatedMatch> chosenMatches;
  private final List<CollatedMatch> potentialMatches;

  public QuantumCollatedMatchList(List<CollatedMatch> chosenMatches, List<CollatedMatch> potentialMatches) {
    this.chosenMatches = Collections.unmodifiableList(chosenMatches);
    this.potentialMatches = Collections.unmodifiableList(potentialMatches);
  }

  public QuantumCollatedMatchList chooseMatch(CollatedMatch match) {
    checkState(potentialMatches.contains(match));

    List<CollatedMatch> newChosen = cloneChosenMatches();
    newChosen.add(match);

    List<CollatedMatch> newPotential = calculateNewPotential(potentialMatches, match);

    return new QuantumCollatedMatchList(newChosen, newPotential);
  }

  public QuantumCollatedMatchList discardMatch(CollatedMatch match) {
    checkState(potentialMatches.contains(match));

    List<CollatedMatch> newChosen = cloneChosenMatches();

    List<CollatedMatch> newPotential = new ArrayList<>(potentialMatches);
    newPotential.remove(match);

    return new QuantumCollatedMatchList(newChosen, newPotential);
  }

  private List<CollatedMatch> cloneChosenMatches() {
    return new ArrayList<>(chosenMatches);
  }

  private List<CollatedMatch> calculateNewPotential(List<CollatedMatch> potentialMatches, CollatedMatch match) {
    List<CollatedMatch> newPotential = new ArrayList<>(potentialMatches);
    List<CollatedMatch> invalidatedMatches = calculateInvalidatedMatches(potentialMatches, match);
    newPotential.removeAll(invalidatedMatches);
    return newPotential;
  }

  public boolean isDetermined() {
    return potentialMatches.isEmpty();
  }

  public int totalSize() {
    return chosenMatches.size() + potentialMatches.size();
  }

  private List<CollatedMatch> calculateInvalidatedMatches(List<CollatedMatch> potentialMatches, CollatedMatch match) {
    TextNode node = match.getCollatedNode();
    TokenVertex tokenVertexForWitness = match.getWitnessVertex();
    int minNodeRank = match.getNodeRank();
    int minVertexRank = match.getVertexRank();

    return potentialMatches.stream()//
        .filter(m -> m.getCollatedNode().equals(node) //
            || m.getWitnessVertex().equals(tokenVertexForWitness) //
            || (hasSigilOverlap(m, node) && m.getNodeRank() < minNodeRank) //
            || m.getVertexRank() < minVertexRank)//
        .collect(toList());
  }

  private boolean hasSigilOverlap(CollatedMatch m, TextNode node) {
    Set<String> nodeSigils = node.getSigils();
    // m and node have witnesses in common
    // for those witnesses they have in common, the branchpath of one is the startsubpath otf the other.
    return m.getSigils().stream().filter(nodeSigils::contains).anyMatch(s ->
        branchPathsOverlap(m.getBranchPath(s), node.getBranchPath(s))
    );
  }

  static boolean branchPathsOverlap(List<Integer> matchBranchPath, List<Integer> nodeBranchPath) {
    int minSize = Math.min(matchBranchPath.size(), nodeBranchPath.size());
    for (int i = 0; i < minSize; i++) {
      if (!matchBranchPath.get(i).equals(nodeBranchPath.get(i))) {
        return false;
      }
    }
    return true;
  }

  public List<CollatedMatch> getChosenMatches() {
    return chosenMatches;
  }

  @Override
  public String toString() {
    return "(" + chosenMatches + " | " + potentialMatches + ")";
  }

  public List<CollatedMatch> getPotentialMatches() {
    return potentialMatches;
  }

  @Override
  public int hashCode() {
    return chosenMatches.hashCode() + potentialMatches.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof QuantumCollatedMatchList)) {
      return false;
    }
    QuantumCollatedMatchList other = (QuantumCollatedMatchList) obj;
    return chosenMatches.equals(other.chosenMatches) && potentialMatches.equals(other.potentialMatches);
  }

}
