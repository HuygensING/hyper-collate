package nl.knaw.huygens.hypercollate.collater;

import eu.interedition.collatex.dekker.astar.AstarAlgorithm;

import java.util.*;

public class OptimalMatchSetAlgorithm extends AstarAlgorithm<QuantumMatchSet, LostPotential> {

  private final Collection<Match> allPotentialMatches;
  private final Integer maxPotential;

  OptimalMatchSetAlgorithm(Collection<Match> allPotentialMatches) {
    maxPotential = allPotentialMatches.size();
    this.allPotentialMatches = allPotentialMatches;
  }

  public Set<Match> getOptimalMatchSet() {
    QuantumMatchSet startNode = new QuantumMatchSet(Collections.EMPTY_SET, new HashSet<>(allPotentialMatches));
    LostPotential startCost = new LostPotential(allPotentialMatches.size());
    List<QuantumMatchSet> winningPath = aStar(startNode, startCost);
    QuantumMatchSet winningGoal = winningPath.get(winningPath.size() - 1);
    return winningGoal.getChosenMatches();
  }

  @Override
  protected boolean isGoal(QuantumMatchSet matchSet) {
    return matchSet.isDetermined();
  }

  @Override
  protected Iterable<QuantumMatchSet> neighborNodes(QuantumMatchSet matchSet) {
    return matchSet.neighborSets();
  }

  @Override
  protected LostPotential heuristicCostEstimate(QuantumMatchSet match) {
    return new LostPotential(maxPotential - match.potentialSize());
  }

  @Override
  protected LostPotential distBetween(QuantumMatchSet match, QuantumMatchSet other) {
    return new LostPotential(Math.abs(match.totalSize() - other.totalSize()));
  }
}
