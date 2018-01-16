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

import com.google.common.base.Stopwatch;
import eu.interedition.collatex.dekker.astar.AstarAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class OptimalMatchSetAlgorithm1 extends AstarAlgorithm<QuantumMatchSet, LostPotential> implements OptimalMatchSetFinder {
  private static final Logger LOG = LoggerFactory.getLogger(OptimalMatchSetAlgorithm1.class);

  private Integer maxPotential;

  @Override
  public String getName() {
    return "Brute-Force";
  }

  /*
   * (non-Javadoc)
   * @see nl.knaw.huygens.hypercollate.collater.OptimalMatchSetFinder#getOptimalMatchSet()
   */
  @Override
  public Set<Match> getOptimalMatchSet(Collection<Match> allPotentialMatches) {
    maxPotential = allPotentialMatches.size();
    QuantumMatchSet startNode = new QuantumMatchSet(Collections.EMPTY_SET, new HashSet<>(allPotentialMatches));
    LostPotential startCost = new LostPotential(0);
    Stopwatch sw = Stopwatch.createStarted();
    List<QuantumMatchSet> winningPath = aStar(startNode, startCost);
    sw.stop();
    LOG.debug("aStar took {} ms", sw.elapsed(TimeUnit.MILLISECONDS));
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
    return new LostPotential(maxPotential - match.totalSize());
  }

  @Override
  protected LostPotential distBetween(QuantumMatchSet match, QuantumMatchSet other) {
    return new LostPotential(Math.abs(match.totalSize() - other.totalSize()));
  }

}
