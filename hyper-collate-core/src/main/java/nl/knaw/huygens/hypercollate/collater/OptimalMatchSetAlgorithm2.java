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

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import eu.interedition.collatex.dekker.astar.AstarAlgorithm;

public class OptimalMatchSetAlgorithm2 extends AstarAlgorithm<QuantumMatchSet, LostPotential> implements OptimalMatchSetFinder {
  private static final Logger LOG = LoggerFactory.getLogger(OptimalMatchSetAlgorithm2.class);

  private List<Match> matchesSortedByWitness1;
  private List<Match> matchesSortedByWitness2;
  private Integer maxPotential;

  @Override
  public Set<Match> getOptimalMatchSet(Collection<Match> allPotentialMatches) {
    maxPotential = allPotentialMatches.size();
    Match aMatch = allPotentialMatches.iterator().next();
    String sigil1 = aMatch.witnessSigils().get(0);
    String sigil2 = aMatch.witnessSigils().get(1);
    matchesSortedByWitness1 = sortMatchesByWitness(allPotentialMatches, sigil1, sigil2);
    matchesSortedByWitness2 = sortMatchesByWitness(allPotentialMatches, sigil2, sigil1);
    QuantumMatchSet startNode = new QuantumMatchSet(Collections.EMPTY_SET, new HashSet<>(allPotentialMatches));
    LostPotential startCost = new LostPotential(0);
    Stopwatch sw = Stopwatch.createStarted();
    List<QuantumMatchSet> winningPath = aStar(startNode, startCost);
    sw.stop();
    LOG.debug("aStar took {} ms", sw.elapsed(TimeUnit.MILLISECONDS));
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
    QuantumMatchSet quantumMatchSet1a = getNextQuantumMatchSet(this.matchesSortedByWitness1, matchSet);
    QuantumMatchSet quantumMatchSet1b = getNextQuantumMatchSetIgnoringNextMatch(this.matchesSortedByWitness1, matchSet);
    QuantumMatchSet quantumMatchSet2a = getNextQuantumMatchSet(this.matchesSortedByWitness2, matchSet);
    QuantumMatchSet quantumMatchSet2b = getNextQuantumMatchSetIgnoringNextMatch(this.matchesSortedByWitness2, matchSet);

    Set<QuantumMatchSet> nextPotentialMatches = new HashSet<>();
    nextPotentialMatches.add(quantumMatchSet1a);
    nextPotentialMatches.add(quantumMatchSet1b);
    nextPotentialMatches.add(quantumMatchSet2a);
    nextPotentialMatches.add(quantumMatchSet2b);
    return nextPotentialMatches;
  }

  private QuantumMatchSet getNextQuantumMatchSet(List<Match> matches, QuantumMatchSet matchSet) {
    Match firstPotentialMatch = getFirstPotentialMatch(matches, matchSet);
    return matchSet.chooseMatch(firstPotentialMatch);
  }

  private QuantumMatchSet getNextQuantumMatchSetIgnoringNextMatch(List<Match> matches, QuantumMatchSet matchSet) {
    Match firstPotentialMatch = getFirstPotentialMatch(matches, matchSet);
    return matchSet.ignoreMatch(firstPotentialMatch);
  }

  private Match getFirstPotentialMatch(List<Match> matches, QuantumMatchSet matchSet) {
    List<Match> potentialMatches = new ArrayList<>();
    potentialMatches.addAll(matches);
    potentialMatches.retainAll(matchSet.getPotentialMatches());
    Match firstPotentialMatch = potentialMatches.get(0);
    return firstPotentialMatch;
  }

  @Override
  protected LostPotential heuristicCostEstimate(QuantumMatchSet match) {
    return new LostPotential(maxPotential - match.totalSize());
  }

  @Override
  protected LostPotential distBetween(QuantumMatchSet match, QuantumMatchSet other) {
    return new LostPotential(Math.abs(match.totalSize() - other.totalSize()));
  }

  @Override
  public String getName() {
    return "Four-Neighbours";
  }
}
