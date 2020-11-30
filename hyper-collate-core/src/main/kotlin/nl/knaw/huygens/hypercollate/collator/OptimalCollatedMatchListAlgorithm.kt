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
    private var groupedByBranchPathForSiglum: Map<String, Map<BranchPath, List<CollatedMatch>>> = mapOf()

    private val quantumCollatedMatchFingerprints: MutableSet<String> = mutableSetOf()

    override val name: String = "Four-Neighbours"

    override fun getOptimalCollatedMatchList(allPotentialMatches: Collection<CollatedMatch>, sigla: List<String>): List<CollatedMatch> {
        val uniqueNodesInMatches = allPotentialMatches.map { it.collatedNode }.distinct().size
        val uniqueVerticesInMatches = allPotentialMatches.map { it.witnessVertex }.distinct().size
        maxPotential = min(uniqueNodesInMatches, uniqueVerticesInMatches)

        matchesSortedByNode = allPotentialMatches.sortedByNode()
        matchesSortedByWitness = allPotentialMatches.sortedByWitness()
        groupedByBranchPathForSiglum = sigla.map { s -> s to allPotentialMatches.groupBy { it.getBranchPath(s) } }.toMap()

        val startNode = QuantumCollatedMatchList(listOf(), allPotentialMatches.toList())
        val startCost = LostPotential(0)
        val sw = Stopwatch.createStarted()
        val winningPath = aStar(startNode, startCost)
        sw.stop()
        log.debug("aStar took {} ms", sw.elapsed(TimeUnit.MILLISECONDS))
        val winningGoal = winningPath.last() ?: error("no winningPath found")
        return winningGoal.chosenMatches
    }

    override fun isGoal(matchList: QuantumCollatedMatchList): Boolean =
            matchList.isDetermined

    override fun neighborNodes(matchList: QuantumCollatedMatchList): Iterable<QuantumCollatedMatchList> = neighborNodes_v2(matchList)

    private fun neighborNodes_v1(matchList: QuantumCollatedMatchList): Iterable<QuantumCollatedMatchList> {
        val nextMatchSequence: MutableList<CollatedMatch> = mutableListOf()
        val cms1 = (matchesSortedByNode - matchList.chosenMatches).filter { it in matchList.potentialMatches }.toMutableList()
        val cms2 = (matchesSortedByWitness - matchList.chosenMatches).filter { it in matchList.potentialMatches }.toMutableList()
        var goOn = cms1.isNotEmpty()
        while (goOn) {
            val next1 = cms1.removeAt(0)
            val next2 = cms2.removeAt(0)
            if (next1 == next2) {
                nextMatchSequence += next1
                goOn = cms1.isNotEmpty()
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

    private fun neighborNodes_v2(matchList: QuantumCollatedMatchList): Iterable<QuantumCollatedMatchList> {
        val relevantMatchesSortedByNode = (matchesSortedByNode - matchList.chosenMatches).asSequence().filter { it in matchList.potentialMatches }
        val matchesByNodeRank = relevantMatchesSortedByNode.groupBy { it.nodeRank }
        val relevantMatchesSortedByWitness = (matchesSortedByWitness - matchList.chosenMatches).asSequence().filter { it in matchList.potentialMatches }
        val matchesByVertexRank = relevantMatchesSortedByWitness.groupBy { it.vertexRank }
        val nextMatchSequence: MutableList<CollatedMatch> = mutableListOf()
        val chosenBranchesPerSiglum: MutableMap<String, MutableSet<BranchPath>> = mutableMapOf()
        matchList.chosenMatches.forEach { m ->
            m.sigla.forEach { s ->
                chosenBranchesPerSiglum
                        .getOrPut(s) { mutableSetOf() }
                        .add(m.getBranchPath(s))
            }
        }

        val nodeRankIterator = matchesByNodeRank.keys.sorted().iterator()
        val vertexRankIterator = matchesByVertexRank.keys.sorted().iterator()
        var goOn = nodeRankIterator.hasNext()
        while (goOn) {
            val nodeRank = nodeRankIterator.next()
            val vertexRank = vertexRankIterator.next()
            val allNextByNode = matchesByNodeRank[nodeRank] ?: error("rank $nodeRank not found in matchesByNodeRank")
            val allNextByVertex = matchesByVertexRank[vertexRank]
                    ?: error("rank $vertexRank not found in matchesByVertexRank")
            val nextByNodeFromChosenBranch = allNextByNode.filter { m ->
                m.sigla.any { s ->
                    m.getBranchPath(s)!!.size > 1 &&
                            chosenBranchesPerSiglum.containsKey(s) &&
                            chosenBranchesPerSiglum[s]!!.isNotEmpty() &&
                            m.getBranchPath(s) in chosenBranchesPerSiglum[s]!!
                }
            }
            val nextByVertexFromChosenBranch = allNextByVertex.filter { m ->
                m.sigla.any { s ->
                    m.getBranchPath(s)!!.size > 1 &&
                            m.getBranchPath(s)!!.size > 1 &&
                            chosenBranchesPerSiglum.containsKey(s) &&
                            chosenBranchesPerSiglum[s]!!.isNotEmpty() &&
                            m.getBranchPath(s) in chosenBranchesPerSiglum[s]!!
                }
            }
            val nextByNode = if (nextByNodeFromChosenBranch.isNotEmpty()) nextByNodeFromChosenBranch else allNextByNode
            val nextByVertex = if (nextByVertexFromChosenBranch.isNotEmpty()) nextByVertexFromChosenBranch else allNextByVertex
            val nextMatch = nextByNode[0]
            if ((nextByNode.size > 1 || nextByVertex.size > 1 || nextMatch != nextByVertex[0]))
                goOn = false
            else {
                nextMatchSequence += nextMatch
                for (s in nextMatch.sigla) {
                    chosenBranchesPerSiglum
                            .getOrPut(s) { mutableSetOf() }
                            .add(nextMatch.getBranchPath(s))
                }
                goOn = nodeRankIterator.hasNext()
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

    class CollatedMatchNodeRankComparator : Comparator<CollatedMatch> {
        override fun compare(o1: CollatedMatch?, o2: CollatedMatch?): Int =
                if (o1 == null || o2 == null) 0
                else o1.nodeRank.compareTo(o2.nodeRank)
    }

    class CollatedMatchVertexRankComparator : Comparator<CollatedMatch> {
        override fun compare(o1: CollatedMatch?, o2: CollatedMatch?): Int =
                if (o1 == null || o2 == null) 0
                else o1.vertexRank.compareTo(o2.vertexRank)
    }

    companion object {
        private val log = LoggerFactory.getLogger(OptimalCollatedMatchListAlgorithm::class.java)

        private val matchNodeComparator = CollatedMatchNodeRankComparator()
                .then(CollatedMatchVertexRankComparator())

        private fun Collection<CollatedMatch>.sortedByNode(): List<CollatedMatch> =
                sortedWith(matchNodeComparator)

        private val matchWitnessComparator = CollatedMatchVertexRankComparator()
                .then(CollatedMatchNodeRankComparator())

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
