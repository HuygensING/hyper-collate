package nl.knaw.huygens.hypercollate.collater;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;
import nl.knaw.huygens.hypercollate.model.TokenVertex;

import java.util.*;
import java.util.stream.Stream;

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

public class QuantumMatchSet {

  private final Set<Match> chosenMatches;
  private final Set<Match> potentialMatches;

  public QuantumMatchSet(Set<Match> chosenMatches, Set<Match> potentialMatches) {
    this.chosenMatches = Collections.unmodifiableSet(chosenMatches);
    this.potentialMatches = Collections.unmodifiableSet(potentialMatches);
  }

  public QuantumMatchSet chooseMatch(Match match) {
    checkState(potentialMatches.contains(match));

    Set<Match> newChosen = cloneChosenMatches();
    newChosen.add(match);

    Set<Match> newPotential = calculateNewPotential(potentialMatches, match);

    return new QuantumMatchSet(newChosen, newPotential);
  }

  public QuantumMatchSet discardMatch(Match match) {
    checkState(potentialMatches.contains(match));

    Set<Match> newChosen = cloneChosenMatches();

    Set<Match> newPotential = new HashSet<>(potentialMatches);
    newPotential.remove(match);

    return new QuantumMatchSet(newChosen, newPotential);
  }

  private Set<Match> cloneChosenMatches() {
    Set<Match> newChosen = new HashSet<>(chosenMatches);
    return newChosen;
  }

  private Set<Match> calculateNewPotential(Collection<Match> potentialMatches, Match match) {
    Set<Match> newPotential = new HashSet<>(potentialMatches);
    List<Match> invalidatedMatches = calculateInvalidatedMatches(potentialMatches, match);
    newPotential.removeAll(invalidatedMatches);
    return newPotential;
  }

  public boolean isDetermined() {
    return potentialMatches.isEmpty();
  }

  public int totalSize() {
    return chosenMatches.size() + potentialMatches.size();
  }

  private List<Match> calculateInvalidatedMatches(Collection<Match> potentialMatches, Match match) {
    List<String> sigils = match.witnessSigils();
    String sigil1 = sigils.get(0);
    String sigil2 = sigils.get(1);
    TokenVertex tokenVertexForWitness1 = match.getTokenVertexForWitness(sigil1);
    TokenVertex tokenVertexForWitness2 = match.getTokenVertexForWitness(sigil2);
    int minRank1 = match.getRankForWitness(sigil1);
    int minRank2 = match.getRankForWitness(sigil2);

    return potentialMatches.stream()//
        .filter(m -> m.getTokenVertexForWitness(sigil1).equals(tokenVertexForWitness1) //
            || m.getTokenVertexForWitness(sigil2).equals(tokenVertexForWitness2) //
            || m.getRankForWitness(sigil1) < minRank1 //
            || m.getRankForWitness(sigil2) < minRank2)//
        .collect(toList());
  }

  public Iterable<QuantumMatchSet> neighborSets() {
    Stream<QuantumMatchSet> stream = potentialMatches.stream()//
        .map(this::chooseMatch);
    return stream::iterator;

  }

  public Set<Match> getChosenMatches() {
    return chosenMatches;
  }

  public Integer potentialSize() {
    return potentialMatches.size();
  }

  @Override
  public String toString() {
    return "(" + chosenMatches + " | " + potentialMatches + ")";
  }

  public Set<Match> getPotentialMatches() {
    return potentialMatches;
  }

  @Override
  public int hashCode() {
    return chosenMatches.hashCode() + potentialMatches.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof QuantumMatchSet)) {
      return false;
    }
    QuantumMatchSet other = (QuantumMatchSet) obj;
    return chosenMatches.equals(other.chosenMatches) && potentialMatches.equals(other.potentialMatches);
  }

}
