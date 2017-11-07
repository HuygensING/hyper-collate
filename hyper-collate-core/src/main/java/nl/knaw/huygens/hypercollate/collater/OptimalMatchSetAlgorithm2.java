package nl.knaw.huygens.hypercollate.collater;

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

import com.google.common.base.Stopwatch;
import eu.interedition.collatex.dekker.astar.AstarAlgorithm;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

public class OptimalMatchSetAlgorithm2 extends AstarAlgorithm<QuantumMatchSet, LostPotential> {

  private final Collection<Match> allPotentialMatches;
  private final List<Match> matchesSortedByWitness1;
  private final List<Match> matchesSortedByWitness2;
  private final Integer maxPotential;

  OptimalMatchSetAlgorithm2(Collection<Match> allPotentialMatches) {
    maxPotential = allPotentialMatches.size();
    this.allPotentialMatches = allPotentialMatches;
    Match aMatch = allPotentialMatches.iterator().next();
    String sigil1 = aMatch.witnessSigils().get(0);
    String sigil2 = aMatch.witnessSigils().get(1);
    matchesSortedByWitness1 = sortMatchesByWitness(allPotentialMatches, sigil1, sigil2);
    matchesSortedByWitness2 = sortMatchesByWitness(allPotentialMatches, sigil2, sigil1);
  }

  public Set<Match> getOptimalMatchSet() {
    QuantumMatchSet startNode = new QuantumMatchSet(Collections.EMPTY_SET, new HashSet<>(allPotentialMatches));
    LostPotential startCost = new LostPotential(0);
    Stopwatch sw = Stopwatch.createStarted();
    List<QuantumMatchSet> winningPath = aStar(startNode, startCost);
    sw.stop();
    System.out.println("aStar took " + sw.elapsed(TimeUnit.MILLISECONDS) + "ms");
    QuantumMatchSet winningGoal = winningPath.get(winningPath.size() - 1);
    return winningGoal.getChosenMatches();
  }

  private static List<Match> sortMatchesByWitness(Collection<Match> matches, //
                                                  String sigil1, //
                                                  String sigil2) {
    Comparator<Match> matchComparator = matchComparator(sigil1, sigil2);
    return matches.stream()//
        .sorted(matchComparator)//
        .collect(toList());
  }

  private static Comparator<Match> matchComparator(String sigil1, //
                                                   String sigil2) {
    return (match1, match2) -> {
      Integer rank1 = match1.getRankForWitness(sigil1);
      Integer rank2 = match2.getRankForWitness(sigil1);
      if (rank1.equals(rank2)) {
        rank1 = match1.getRankForWitness(sigil2);
        rank2 = match2.getRankForWitness(sigil2);
      }
      return rank1.compareTo(rank2);
    };
  }

  @Override
  protected boolean isGoal(QuantumMatchSet matchSet) {
    return matchSet.isDetermined();
  }

  @Override
  protected Iterable<QuantumMatchSet> neighborNodes(QuantumMatchSet matchSet) {
    QuantumMatchSet quantumMatchSet1 = getNextQuantumMatchSet(this.matchesSortedByWitness1, matchSet);
    QuantumMatchSet quantumMatchSet2 = getNextQuantumMatchSet(this.matchesSortedByWitness2, matchSet);

    Set<QuantumMatchSet> nextPotentialMatches = new HashSet<>();
    nextPotentialMatches.add(quantumMatchSet1);
    nextPotentialMatches.add(quantumMatchSet2);
    return nextPotentialMatches;
  }

  private QuantumMatchSet getNextQuantumMatchSet(List<Match> matches, QuantumMatchSet matchSet) {
    List<Match> potentialMatches = new ArrayList<>();
    potentialMatches.addAll(matches);
    potentialMatches.retainAll(matchSet.getPotentialMatches());
    Match firstPotentialMatch = potentialMatches.get(0);
    return matchSet.chooseMatch(firstPotentialMatch);
  }

  @Override
  protected LostPotential heuristicCostEstimate(QuantumMatchSet match) {
    return new LostPotential(maxPotential - match.totalSize());
  }

  @Override
  protected LostPotential distBetween(QuantumMatchSet match, QuantumMatchSet other) {
    return new LostPotential(Math.abs(match.totalSize() - other.totalSize()));
  }
}
