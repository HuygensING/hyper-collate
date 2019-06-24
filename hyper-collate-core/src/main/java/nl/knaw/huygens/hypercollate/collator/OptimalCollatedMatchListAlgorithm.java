package nl.knaw.huygens.hypercollate.collator;

/*-
 * #%L
 * hyper-collate-core
 * =======
 * Copyright (C) 2017 - 2019 Huygens ING (KNAW)
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
import static java.util.stream.Collectors.toList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class OptimalCollatedMatchListAlgorithm extends AstarAlgorithm<QuantumCollatedMatchList, LostPotential> implements OptimalCollatedMatchListFinder {
  private static final Logger LOG = LoggerFactory.getLogger(OptimalCollatedMatchListAlgorithm.class);

  private List<CollatedMatch> matchesSortedByNode;
  private List<CollatedMatch> matchesSortedByWitness;
  private Integer maxPotential;

  @Override
  public String getName() {
    return "Four-Neighbours";
  }

  @Override
  public List<CollatedMatch> getOptimalCollatedMatchList(Collection<CollatedMatch> allPotentialMatches) {
    maxPotential = allPotentialMatches.size();
    matchesSortedByNode = sortMatchesByNode(allPotentialMatches);
    matchesSortedByWitness = sortMatchesByWitness(allPotentialMatches);
    QuantumCollatedMatchList startNode = new QuantumCollatedMatchList(Collections.EMPTY_LIST, new ArrayList<>(allPotentialMatches));
    LostPotential startCost = new LostPotential(0);
    Stopwatch sw = Stopwatch.createStarted();
    List<QuantumCollatedMatchList> winningPath = aStar(startNode, startCost);
    sw.stop();
    LOG.debug("aStar took {} ms", sw.elapsed(TimeUnit.MILLISECONDS));
    QuantumCollatedMatchList winningGoal = winningPath.get(winningPath.size() - 1);
    return new ArrayList<>(winningGoal.getChosenMatches());
  }

  private static List<CollatedMatch> sortMatchesByNode(Collection<CollatedMatch> matches) {
    Comparator<CollatedMatch> matchComparator = Comparator.comparing(CollatedMatch::getNodeRank).thenComparing(CollatedMatch::getVertexRank);
    return sortMatches(matches, matchComparator);
  }

  private static List<CollatedMatch> sortMatchesByWitness(Collection<CollatedMatch> matches) {
    Comparator<CollatedMatch> matchComparator = Comparator.comparing(CollatedMatch::getVertexRank).thenComparing(CollatedMatch::getNodeRank);
    return sortMatches(matches, matchComparator);
  }

  private static List<CollatedMatch> sortMatches(Collection<CollatedMatch> matches, Comparator<CollatedMatch> matchComparator) {
    return matches.stream()//
//        .peek(System.out::println)//
        .sorted(matchComparator)//
        .collect(toList());
  }

  @Override
  protected boolean isGoal(QuantumCollatedMatchList matchList) {
    return matchList.isDetermined();
  }

  @Override
  protected Iterable<QuantumCollatedMatchList> neighborNodes(QuantumCollatedMatchList matchList) {
    Set<QuantumCollatedMatchList> nextPotentialMatches = new LinkedHashSet<>();

    CollatedMatch firstPotentialMatch1 = getFirstPotentialMatch(this.matchesSortedByNode, matchList);
    addNeighborNodes(matchList, nextPotentialMatches, firstPotentialMatch1);

    List<CollatedMatch> matchesSortedByWitness = this.matchesSortedByWitness;
    CollatedMatch firstPotentialMatch2 = getFirstPotentialMatch(matchesSortedByWitness, matchList);
    if (!firstPotentialMatch1.equals(firstPotentialMatch2)) {
      addNeighborNodes(matchList, nextPotentialMatches, firstPotentialMatch2);
    }

    return nextPotentialMatches;
  }

  private void addNeighborNodes(QuantumCollatedMatchList matchList, Set<QuantumCollatedMatchList> nextPotentialMatches, CollatedMatch firstPotentialMatch) {
    QuantumCollatedMatchList quantumMatchSet1 = matchList.chooseMatch(firstPotentialMatch);
    QuantumCollatedMatchList quantumMatchSet2 = matchList.discardMatch(firstPotentialMatch);
    nextPotentialMatches.add(quantumMatchSet1);
    nextPotentialMatches.add(quantumMatchSet2);
  }

  private CollatedMatch getFirstPotentialMatch(List<CollatedMatch> matches, QuantumCollatedMatchList matchSet) {
    List<CollatedMatch> potentialMatches = new ArrayList<>(matches);
    potentialMatches.retainAll(matchSet.getPotentialMatches());
    return potentialMatches.get(0);
  }

  @Override
  protected LostPotential heuristicCostEstimate(QuantumCollatedMatchList matchList) {
    return new LostPotential(maxPotential - matchList.totalSize());
  }

  @Override
  protected LostPotential distBetween(QuantumCollatedMatchList matchList0, QuantumCollatedMatchList matchList1) {
    return new LostPotential(Math.abs(matchList0.totalSize() - matchList1.totalSize()));
  }

}
