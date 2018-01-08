package nl.knaw.huygens.hypercollate.collater;

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

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import nl.knaw.huygens.hypercollate.model.CollationGraph;
import nl.knaw.huygens.hypercollate.model.TokenVertex;

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
    List<CollatedMatch> newChosen = new ArrayList<>(chosenMatches);
    return newChosen;
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
    CollationGraph.Node node = match.getCollatedNode();
    TokenVertex tokenVertexForWitness = match.getWitnessVertex();
    Set<String> nodeSigils = node.getSigils();
    int minNodeRank = match.getNodeRank();
    int minVertexRank = match.getVertexRank();

    return potentialMatches.stream()//
        .filter(m -> m.getCollatedNode().equals(node) //
            || m.getWitnessVertex().equals(tokenVertexForWitness) //
            || (hasSigilOverlap(m, nodeSigils) && m.getNodeRank() < minNodeRank) //
            || m.getVertexRank() < minVertexRank)//
        .collect(toList());
  }

  private boolean hasSigilOverlap(CollatedMatch m, Set<String> nodeSigils) {
    return m.getSigils().stream().anyMatch(nodeSigils::contains);
  }

  public Iterable<QuantumCollatedMatchList> neighborSets() {
    Stream<QuantumCollatedMatchList> stream = potentialMatches.stream()//
        .map(this::chooseMatch);
    return stream::iterator;
  }

  public List<CollatedMatch> getChosenMatches() {
    return chosenMatches;
  }

  public Integer potentialSize() {
    return potentialMatches.size();
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
