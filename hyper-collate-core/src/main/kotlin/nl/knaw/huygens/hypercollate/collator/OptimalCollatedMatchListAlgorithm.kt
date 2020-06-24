package nl.knaw.huygens.hypercollate.collator

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

import com.google.common.base.Stopwatch
import eu.interedition.collatex.dekker.astar.AstarAlgorithm
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class OptimalCollatedMatchListAlgorithm : AstarAlgorithm<QuantumCollatedMatchList, LostPotential>(), OptimalCollatedMatchListFinder {
    private var matchesSortedByNode: List<CollatedMatch> = listOf()
    private var matchesSortedByWitness: List<CollatedMatch> = listOf()
    private var maxPotential: Int? = null

    override val name: String
        get() = "Four-Neighbours"

    override fun getOptimalCollatedMatchList(allPotentialMatches: Collection<CollatedMatch>): MutableList<CollatedMatch> {
        maxPotential = allPotentialMatches.size
        matchesSortedByNode = sortMatchesByNode(allPotentialMatches)
        matchesSortedByWitness = sortMatchesByWitness(allPotentialMatches)
        val startNode = QuantumCollatedMatchList(listOf(), ArrayList(allPotentialMatches))
        val startCost = LostPotential(0)
        val sw = Stopwatch.createStarted()
        val winningPath = aStar(startNode, startCost)
        sw.stop()
        LOG.debug("aStar took {} ms", sw.elapsed(TimeUnit.MILLISECONDS))
        val winningGoal = winningPath[winningPath.size - 1] ?: error("no winningPath found")
        return ArrayList(winningGoal.chosenMatches)
    }

    override fun isGoal(matchList: QuantumCollatedMatchList): Boolean =
            matchList.isDetermined

    override fun neighborNodes(matchList: QuantumCollatedMatchList): Iterable<QuantumCollatedMatchList> {
        val nextPotentialMatches: MutableSet<QuantumCollatedMatchList> = LinkedHashSet()
        val firstPotentialMatch1 = getFirstPotentialMatch(matchesSortedByNode, matchList)
        addNeighborNodes(matchList, nextPotentialMatches, firstPotentialMatch1)
        val matchesSortedByWitness = matchesSortedByWitness
        val firstPotentialMatch2 = getFirstPotentialMatch(matchesSortedByWitness, matchList)
        if (firstPotentialMatch1 != firstPotentialMatch2) {
            addNeighborNodes(matchList, nextPotentialMatches, firstPotentialMatch2)
        }
        return nextPotentialMatches
    }

    private fun addNeighborNodes(
            matchList: QuantumCollatedMatchList,
            nextPotentialMatches: MutableSet<QuantumCollatedMatchList>,
            firstPotentialMatch: CollatedMatch) {
        // TODO: more neighbournodes: find a set of matches with same potential
        // TODO: neighbournodes op basis van matching tokens
        val quantumMatchSet1 = matchList.chooseMatch(firstPotentialMatch)
        val quantumMatchSet2 = matchList.discardMatch(firstPotentialMatch)
        nextPotentialMatches.add(quantumMatchSet1)
        nextPotentialMatches.add(quantumMatchSet2)
    }

    private fun getFirstPotentialMatch(matches: List<CollatedMatch>, matchSet: QuantumCollatedMatchList): CollatedMatch {
        val potentialMatches: MutableList<CollatedMatch> = matches.toMutableList()
        potentialMatches.retainAll(matchSet.potentialMatches)
        return potentialMatches[0]
    }

    override fun heuristicCostEstimate(matchList: QuantumCollatedMatchList): LostPotential =
            LostPotential(maxPotential!! - matchList.totalSize)

    override fun distBetween(matchList0: QuantumCollatedMatchList, matchList1: QuantumCollatedMatchList): LostPotential =
            LostPotential(abs(matchList0.totalSize - matchList1.totalSize))

    companion object {
        private val LOG = LoggerFactory.getLogger(OptimalCollatedMatchListAlgorithm::class.java)

        private val matchNodeComparator = Comparator.comparing { obj: CollatedMatch -> obj.nodeRank }
                .thenComparing { obj: CollatedMatch -> obj.vertexRank }

        private fun sortMatchesByNode(matches: Collection<CollatedMatch>): List<CollatedMatch> =
                matches.sortedWith(matchNodeComparator)

        private val matchWitnessComparator = Comparator.comparing { obj: CollatedMatch -> obj.vertexRank }
                .thenComparing { obj: CollatedMatch -> obj.nodeRank }

        private fun sortMatchesByWitness(matches: Collection<CollatedMatch>): List<CollatedMatch> =
                matches.sortedWith(matchWitnessComparator)
    }
}
