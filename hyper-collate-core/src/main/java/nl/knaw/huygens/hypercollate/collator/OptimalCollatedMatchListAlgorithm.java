package nl.knaw.huygens.hypercollate.collator;

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

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import eu.interedition.collatex.dekker.astar.AstarAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class OptimalCollatedMatchListAlgorithm extends AstarAlgorithm<QuantumCollatedMatchList, LostPotential> implements OptimalCollatedMatchListFinder {
  private static final Logger LOG = LoggerFactory.getLogger(OptimalCollatedMatchListAlgorithm.class);
  private static final Boolean SHORTCUT = true; // Reduce the decision tree. Experimental, might lose alternatives.

  private List<CollatedMatch> matchesSortedByNode;
  private List<CollatedMatch> matchesSortedByWitness;
  private Integer maxPotential;

  private Map<QuantumCollatedMatchList, Set<QuantumCollatedMatchList>> neighborNodeMap = new HashMap<>();
  private QuantumCollatedMatchList rootNode;

  @Override
  public String getName() {
    return "Max-Four-Neighbours";
  } // Because each decision node has at most 4 neighbour nodes

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

  private AtomicInteger decisionTreeNodeCounter = new AtomicInteger();
  private Map<QuantumCollatedMatchList, Integer> decisionTreeNodeNumberMap = new HashMap<>();

  protected Iterable<QuantumCollatedMatchList> neighborNodes(QuantumCollatedMatchList matchList) {
    if (rootNode == null) {
      rootNode = matchList;
      decisionTreeNodeNumberMap.put(matchList, decisionTreeNodeCounter.getAndIncrement());
    }

    Set<QuantumCollatedMatchList> nextPotentialMatches = new LinkedHashSet<>();

    CollatedMatch firstPotentialMatch1 = getFirstPotentialMatch(this.matchesSortedByNode, matchList);
    CollatedMatch firstPotentialMatch2 = getFirstPotentialMatch(this.matchesSortedByWitness, matchList);

    if (SHORTCUT && firstPotentialMatch1.equals(firstPotentialMatch2)) {
      keepChoosingMatchesUntilPotentialsAreDifferent(matchList, nextPotentialMatches, firstPotentialMatch1);

    } else {
      addNeighborNodes(matchList, nextPotentialMatches, firstPotentialMatch1);
      addNeighborNodes(matchList, nextPotentialMatches, firstPotentialMatch2);
    }

    Set<QuantumCollatedMatchList> newPotentialMatches = nextPotentialMatches.stream()//
        .filter(m -> !neighborNodeMap.containsKey(m))//
        .collect(toSet());

    neighborNodeMap.put(matchList, newPotentialMatches);
    newPotentialMatches.forEach(l -> decisionTreeNodeNumberMap.putIfAbsent(l, decisionTreeNodeCounter.getAndIncrement()));

    return newPotentialMatches;
  }

  private void keepChoosingMatchesUntilPotentialsAreDifferent(QuantumCollatedMatchList matchList, Set<QuantumCollatedMatchList> nextPotentialMatches, CollatedMatch firstPotentialMatch1) {
    CollatedMatch firstPotentialMatch2;
    QuantumCollatedMatchList quantumCollatedMatchList = matchList;
    boolean goOn = true;
    while (goOn) {
      quantumCollatedMatchList = quantumCollatedMatchList.chooseMatch(firstPotentialMatch1);
      if (quantumCollatedMatchList.isDetermined()) {
        goOn = false;
      } else {
        firstPotentialMatch1 = getFirstPotentialMatch(this.matchesSortedByNode, quantumCollatedMatchList);
        firstPotentialMatch2 = getFirstPotentialMatch(this.matchesSortedByWitness, quantumCollatedMatchList);
        goOn = firstPotentialMatch1.equals(firstPotentialMatch2);
      }
    }
    nextPotentialMatches.add(quantumCollatedMatchList);
  }

//  protected Iterable<QuantumCollatedMatchList> neighborNodes0(QuantumCollatedMatchList matchList) {
//    LOG.info("neighborNodes: matchList = {}", matchList);
//    if (rootNode == null) {
//      rootNode = matchList;
//      decisionTreeNodeNumberMap.put(matchList, decisionTreeNodeCounter.getAndIncrement());
//    }
//
//    Set<QuantumCollatedMatchList> nextPotentialMatches = new LinkedHashSet<>();
//
//    CollatedMatch firstPotentialMatch1 = getFirstPotentialMatch(this.matchesSortedByNode, matchList);
//    CollatedMatch firstPotentialMatch2 = getFirstPotentialMatch(this.matchesSortedByWitness, matchList);
//
//    addNeighborNodes(matchList, nextPotentialMatches, firstPotentialMatch1);
//    if (firstPotentialMatch1.equals(firstPotentialMatch2)) {
//      //TODO
//    } else {
//      addNeighborNodes(matchList, nextPotentialMatches, firstPotentialMatch2);
//    }
//
//    Set<QuantumCollatedMatchList> newPotentialMatches = nextPotentialMatches.stream()//
//        .filter(m -> !neighborNodeMap.containsKey(m))//
//        .collect(toSet());
//
//    neighborNodeMap.put(matchList, newPotentialMatches);
//    newPotentialMatches.forEach(l -> decisionTreeNodeNumberMap.putIfAbsent(l, decisionTreeNodeCounter.getAndIncrement()));
//
//    return nextPotentialMatches;
//  }

  private CollatedMatch getFirstPotentialMatch(List<CollatedMatch> matches, QuantumCollatedMatchList matchSet) {
    List<CollatedMatch> potentialMatches = new ArrayList<>(matches);
    potentialMatches.retainAll(matchSet.getPotentialMatches());
    return potentialMatches.get(0);
  }

  private void addNeighborNodes(QuantumCollatedMatchList matchList, Set<QuantumCollatedMatchList> nextPotentialMatches, CollatedMatch firstPotentialMatch) {
    QuantumCollatedMatchList quantumMatchSet1 = matchList.chooseMatch(firstPotentialMatch);
    QuantumCollatedMatchList quantumMatchSet2 = matchList.discardMatch(firstPotentialMatch);
    nextPotentialMatches.add(quantumMatchSet1);
    nextPotentialMatches.add(quantumMatchSet2);
  }

  @Override
  protected LostPotential heuristicCostEstimate(QuantumCollatedMatchList matchList) {
    return new LostPotential(maxPotential - matchList.totalSize());
  }

  @Override
  protected LostPotential distBetween(QuantumCollatedMatchList matchList0, QuantumCollatedMatchList matchList1) {
    return new LostPotential(Math.abs(matchList0.totalSize() - matchList1.totalSize()));
  }

  public DecisionTreeNode getDecisionTreeRootNode() {
    Preconditions.checkNotNull(rootNode, "aStar() needs to be called first.");
    Set<QuantumCollatedMatchList> visitedNodes = new HashSet<>();
    return getConnectedDecisionTreeNode(rootNode, visitedNodes);
  }

  private DecisionTreeNode getConnectedDecisionTreeNode(QuantumCollatedMatchList node, Set<QuantumCollatedMatchList> nodesVisited) {
    DecisionTreeNode decisionTreeNode = DecisionTreeNode.of(node)
        .setCost(heuristicCostEstimate(node).getCost())
        .setNumber(decisionTreeNodeNumberMap.get(node));
    if (!nodesVisited.contains(node)) {
      nodesVisited.add(node);
      neighborNodeMap.getOrDefault(node, Collections.emptySet())//
          .stream()//
          .map(n -> getConnectedDecisionTreeNode(n, nodesVisited))//
          .forEach(decisionTreeNode::addChildNode);
    }
    return decisionTreeNode;
  }

}
