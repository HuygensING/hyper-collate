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
import kotlin.math.min

class OptimalCollatedMatchListAlgorithm : AstarAlgorithm<QuantumCollatedMatchList, LostPotential>(), OptimalCollatedMatchListFinder {
    private var matchesSortedByNode: List<CollatedMatch> = listOf()
    private var matchesSortedByWitness: List<CollatedMatch> = listOf()
    private var maxPotential: Int? = null

    private val quantumCollatedMatchFingerprints: MutableSet<String> = mutableSetOf()

    override val name: String
        get() = "Four-Neighbours"

    override fun getOptimalCollatedMatchList(allPotentialMatches: Collection<CollatedMatch>): MutableList<CollatedMatch> {
        val uniqueNodesInMatches = allPotentialMatches.map { it.collatedNode }.distinct().size
        val uniqueVerticesInMatches = allPotentialMatches.map { it.witnessVertex }.distinct().size
        maxPotential = min(uniqueNodesInMatches, uniqueVerticesInMatches)

        matchesSortedByNode = allPotentialMatches.sortedByNode()
        matchesSortedByWitness = allPotentialMatches.sortedByWitness()
        val startNode = QuantumCollatedMatchList(listOf(), allPotentialMatches.toList())
        val startCost = LostPotential(0)
        val sw = Stopwatch.createStarted()
        val winningPath = aStar(startNode, startCost)
        sw.stop()
        LOG.debug("aStar took {} ms", sw.elapsed(TimeUnit.MILLISECONDS))
        val winningGoal = winningPath.last() ?: error("no winningPath found")
        return winningGoal.chosenMatches.toMutableList()
    }

    override fun isGoal(matchList: QuantumCollatedMatchList): Boolean =
            matchList.isDetermined

    override fun neighborNodes(matchList: QuantumCollatedMatchList): Iterable<QuantumCollatedMatchList> {
        val nextMatchSequence: MutableList<CollatedMatch> = mutableListOf()
        val cms1 = (matchesSortedByNode - matchList.chosenMatches).asSequence().filter { it in matchList.potentialMatches }.iterator()
        val cms2 = (matchesSortedByWitness - matchList.chosenMatches).asSequence().filter { it in matchList.potentialMatches }.iterator()
        var goOn = cms1.hasNext()
        while (goOn) {
            val next1 = cms1.next()
            val next2 = cms2.next()
            if (next1 == next2) {
                nextMatchSequence += next1
                goOn = cms1.hasNext()
            } else {
                goOn = false
            }
        }
        val nextPotentialMatches: MutableSet<QuantumCollatedMatchList> = mutableSetOf()
        if (nextMatchSequence.isEmpty()) {
            val firstPotentialMatch1 = matchesSortedByNode.firstPotentialMatch(matchList)
            nextPotentialMatches.addNeighborNodes(matchList, firstPotentialMatch1)
            val firstPotentialMatch2 = matchesSortedByWitness.firstPotentialMatch(matchList)
            if (firstPotentialMatch1 != firstPotentialMatch2) {
                nextPotentialMatches.addNeighborNodes(matchList, firstPotentialMatch2)
            }
        } else {
            nextPotentialMatches.addNeighborNodes(matchList, nextMatchSequence)
        }
        return nextPotentialMatches
    }

    override fun heuristicCostEstimate(matchList: QuantumCollatedMatchList): LostPotential =
            LostPotential(maxPotential!! - matchList.maxPotentialSize())

    override fun distBetween(matchList0: QuantumCollatedMatchList, matchList1: QuantumCollatedMatchList): LostPotential =
            LostPotential(abs(matchList0.maxPotentialSize() - matchList1.maxPotentialSize()))

    private fun MutableSet<QuantumCollatedMatchList>.addNeighborNodes(
            matchList: QuantumCollatedMatchList,
            firstPotentialMatch: CollatedMatch
    ) {
        val quantumMatchSet1 = matchList.chooseMatch(firstPotentialMatch)
        val fingerprint1 = quantumMatchSet1.fingerprint
        if (fingerprint1 !in quantumCollatedMatchFingerprints) {
            this += quantumMatchSet1
            quantumCollatedMatchFingerprints += fingerprint1
        }
        val quantumMatchSet2 = matchList.discardMatch(firstPotentialMatch)
        val fingerprint2 = quantumMatchSet2.fingerprint
        if (fingerprint2 !in quantumCollatedMatchFingerprints) {
            this += quantumMatchSet2
            quantumCollatedMatchFingerprints += fingerprint2
        }
    }

    private fun MutableSet<QuantumCollatedMatchList>.addNeighborNodes(
            matchList: QuantumCollatedMatchList,
            matchSequence: List<CollatedMatch>
    ) {
        var chooseMatchSet = matchList
        var discardMatchSet = matchList
        for (cm in matchSequence) {
            if (cm in chooseMatchSet.potentialMatches) {
                chooseMatchSet = chooseMatchSet.chooseMatch(cm)
                discardMatchSet = discardMatchSet.discardMatch(cm)
            }
        }
        val fingerprint1 = chooseMatchSet.fingerprint
        if (fingerprint1 !in quantumCollatedMatchFingerprints) {
            this += chooseMatchSet
            quantumCollatedMatchFingerprints += fingerprint1
        }
        val fingerprint2 = discardMatchSet.fingerprint
        if (fingerprint2 !in quantumCollatedMatchFingerprints) {
            this += discardMatchSet
            quantumCollatedMatchFingerprints += fingerprint2
        }
    }

    private fun List<CollatedMatch>.firstPotentialMatch(matchList: QuantumCollatedMatchList): CollatedMatch =
            first { it in matchList.potentialMatches }

    private fun QuantumCollatedMatchList.maxPotentialSize(): Int {
        val uniquePotentialNodeMatches = this.potentialMatches.map { it.collatedNode }.distinct().size
        val uniquePotentialVertexMatches = this.potentialMatches.map { it.witnessVertex }.distinct().size
        return this.chosenMatches.size + min(uniquePotentialNodeMatches, uniquePotentialVertexMatches)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(OptimalCollatedMatchListAlgorithm::class.java)

        private val matchNodeComparator = Comparator.comparing { obj: CollatedMatch -> obj.nodeRank }
                .thenComparing { obj: CollatedMatch -> obj.vertexRank }

        private fun Collection<CollatedMatch>.sortedByNode(): List<CollatedMatch> =
                sortedWith(matchNodeComparator)

        private val matchWitnessComparator = Comparator.comparing(CollatedMatch::vertexRank)
                .thenComparing(CollatedMatch::nodeRank)

        private fun Collection<CollatedMatch>.sortedByWitness(): List<CollatedMatch> =
                sortedWith(matchWitnessComparator)

        private fun <A> runWithStopwatch(stopwatch: Stopwatch, func: () -> A): A {
            stopwatch.start()
            val result = func()
            stopwatch.stop()
            return result
        }
    }
}
