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
    private val profiler = Profiler()
    private val quantumCollatedMatchFingerprints: MutableSet<String> = mutableSetOf()

    class Profiler() {
        var isGoalCalled = 0
        val isGoalStopWatch: Stopwatch = Stopwatch.createUnstarted()

        var neighborNodesCalled = 0
        val neighborNodesStopWatch: Stopwatch = Stopwatch.createUnstarted()

        var heuristicCostEstimateCalled = 0
        val heuristicCostEstimateStopwatch: Stopwatch = Stopwatch.createUnstarted()

        var distBetweenCalled = 0
        val distBetweenStopwatch: Stopwatch = Stopwatch.createUnstarted()

        var decisionTreeNodes = 0

        override fun toString(): String = """
            |               isGoal(): times called: $isGoalCalled; total time spent: ${isGoalStopWatch.elapsed(TimeUnit.MILLISECONDS)} ms
            |        neighborNodes(): times called: $neighborNodesCalled; total time spent: ${neighborNodesStopWatch.elapsed(TimeUnit.MILLISECONDS)} ms
            |heuristicCostEstimate(): times called: $heuristicCostEstimateCalled; total time spent: ${heuristicCostEstimateStopwatch.elapsed(TimeUnit.MILLISECONDS)} ms
            |          distBetween(): times called: $distBetweenCalled; total time spent: ${distBetweenStopwatch.elapsed(TimeUnit.MILLISECONDS)} ms
            |      decisionTreeNodes: $decisionTreeNodes
        """.trimMargin()
    }

    override val name: String
        get() = "Four-Neighbours"

    override fun getOptimalCollatedMatchList(allPotentialMatches: Collection<CollatedMatch>): MutableList<CollatedMatch> {
        val uniqueNodesInMatches = allPotentialMatches.map { it.collatedNode }.distinct().size
        val uniqueVerticesInMatches = allPotentialMatches.map { it.witnessVertex }.distinct().size
        maxPotential = min(uniqueNodesInMatches, uniqueVerticesInMatches)
        println("allPotentialMatches.size=${allPotentialMatches.size} uniqueNodes=$uniqueNodesInMatches, uniqueVertices=$uniqueVerticesInMatches, maxPotential=$maxPotential")

        matchesSortedByNode = sortMatchesByNode(allPotentialMatches)
        matchesSortedByWitness = sortMatchesByWitness(allPotentialMatches)
        val startNode = QuantumCollatedMatchList(listOf(), ArrayList(allPotentialMatches))
        val startCost = LostPotential(0)
        val sw = Stopwatch.createStarted()
        val winningPath = aStar(startNode, startCost)
        sw.stop()
        LOG.debug("aStar took {} ms", sw.elapsed(TimeUnit.MILLISECONDS))
        val winningGoal = winningPath.last() ?: error("no winningPath found")
        println(profiler)
        return ArrayList(winningGoal.chosenMatches)
    }

    override fun isGoal(matchList: QuantumCollatedMatchList): Boolean {
        profiler.isGoalCalled += 1
        return runWithStopwatch(profiler.isGoalStopWatch) {
            matchList.isDetermined
        }
    }

    override fun neighborNodes(matchList: QuantumCollatedMatchList): Iterable<QuantumCollatedMatchList> {
        profiler.neighborNodesCalled += 1
        val nextPotentialMatches = runWithStopwatch(profiler.neighborNodesStopWatch) {
            val nextPotentialMatches: MutableSet<QuantumCollatedMatchList> = mutableSetOf()
            val nextMatchSequence: MutableList<CollatedMatch> = mutableListOf()
            val cms1 = matchesSortedByNode.asSequence().filter { matchList.potentialMatches.contains(it) }.iterator()
            val cms2 = matchesSortedByWitness.asSequence().filter { matchList.potentialMatches.contains(it) }.iterator()
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
            if (nextMatchSequence.isEmpty()) {
                val firstPotentialMatch1 = getFirstPotentialMatchSequence(matchesSortedByNode, matchList)
                addNeighborNodes(matchList, nextPotentialMatches, firstPotentialMatch1)
                val firstPotentialMatch2 = getFirstPotentialMatchSequence(matchesSortedByWitness, matchList)
                if (firstPotentialMatch1 != firstPotentialMatch2) {
                    addNeighborNodes(matchList, nextPotentialMatches, firstPotentialMatch2)
                }
            } else {
                addNeighborNodes(matchList, nextPotentialMatches, nextMatchSequence)
            }
            nextPotentialMatches
        }

        profiler.decisionTreeNodes += nextPotentialMatches.size
        return nextPotentialMatches
    }

    override fun heuristicCostEstimate(matchList: QuantumCollatedMatchList): LostPotential {
        profiler.heuristicCostEstimateCalled += 1
        return runWithStopwatch(profiler.heuristicCostEstimateStopwatch) {
            LostPotential(maxPotential!! - matchList.maxPotentialSize())
        }
    }

    override fun distBetween(matchList0: QuantumCollatedMatchList, matchList1: QuantumCollatedMatchList): LostPotential {
        profiler.distBetweenCalled += 1
        return runWithStopwatch(profiler.distBetweenStopwatch) {
            LostPotential(abs(matchList0.maxPotentialSize() - matchList1.maxPotentialSize()))
        }
    }

    fun neighborNodes0(matchList: QuantumCollatedMatchList): Iterable<QuantumCollatedMatchList> {
        profiler.neighborNodesCalled += 1
        val nextPotentialMatches: MutableSet<QuantumCollatedMatchList> = LinkedHashSet()
        val firstPotentialMatch1 = getFirstPotentialMatchSequence(matchesSortedByNode, matchList)
        addNeighborNodes(matchList, nextPotentialMatches, firstPotentialMatch1)
        val firstPotentialMatch2 = getFirstPotentialMatchSequence(matchesSortedByWitness, matchList)
        if (firstPotentialMatch1 != firstPotentialMatch2) {
            addNeighborNodes(matchList, nextPotentialMatches, firstPotentialMatch2)
        }
        profiler.decisionTreeNodes += nextPotentialMatches.size
        return nextPotentialMatches
    }

    private fun addNeighborNodes(
            matchList: QuantumCollatedMatchList,
            nextPotentialMatches: MutableSet<QuantumCollatedMatchList>,
            firstPotentialMatch: CollatedMatch) {
        val quantumMatchSet1 = matchList.chooseMatch(firstPotentialMatch)
        val fingerprint1 = quantumMatchSet1.fingerprint
        if (!quantumCollatedMatchFingerprints.contains(fingerprint1)) {
            nextPotentialMatches.add(quantumMatchSet1)
            quantumCollatedMatchFingerprints.add(fingerprint1)
        } else {
//            println("cache hit: $fingerprint1")
        }
        val quantumMatchSet2 = matchList.discardMatch(firstPotentialMatch)
        val fingerprint2 = quantumMatchSet2.fingerprint
        if (!quantumCollatedMatchFingerprints.contains(fingerprint2)) {
            nextPotentialMatches.add(quantumMatchSet2)
            quantumCollatedMatchFingerprints.add(fingerprint2)
        } else {
//            println("cache hit: $fingerprint2")
        }
    }

    private fun addNeighborNodes(
            matchList: QuantumCollatedMatchList,
            nextPotentialMatches: MutableSet<QuantumCollatedMatchList>,
            matchSequence: List<CollatedMatch>) {
        var chooseMatchSet = matchList
        var discardMatchSet = matchList
        matchSequence.forEach { cm ->
            if (chooseMatchSet.potentialMatches.contains(cm)) {
                chooseMatchSet = chooseMatchSet.chooseMatch(cm)
                discardMatchSet = discardMatchSet.discardMatch(cm)
            }
        }
        val fingerprint1 = chooseMatchSet.fingerprint
        if (!quantumCollatedMatchFingerprints.contains(fingerprint1)) {
            nextPotentialMatches.add(chooseMatchSet)
            quantumCollatedMatchFingerprints.add(fingerprint1)
        } else {
//            println("cache hit: $fingerprint1")
        }
        val fingerprint2 = discardMatchSet.fingerprint
        if (!quantumCollatedMatchFingerprints.contains(fingerprint2)) {
            nextPotentialMatches.add(discardMatchSet)
            quantumCollatedMatchFingerprints.add(fingerprint2)
        } else {
//            println("cache hit: $fingerprint2")
        }
    }

    private fun getFirstPotentialMatchSequence(matches: List<CollatedMatch>, matchList: QuantumCollatedMatchList): CollatedMatch =
            matches.first { matchList.potentialMatches.contains(it) }

    private fun QuantumCollatedMatchList.maxPotentialSize(): Int {
        val uniquePotentialNodeMatches = this.potentialMatches.map { it.collatedNode }.distinct().size
        val uniquePotentialVertexMatches = this.potentialMatches.map { it.witnessVertex }.distinct().size
        return this.chosenMatches.size + min(uniquePotentialNodeMatches, uniquePotentialVertexMatches)
    }

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

        private fun <A> runWithStopwatch(stopwatch: Stopwatch, func: () -> A): A {
            stopwatch.start()
            val result = func()
            stopwatch.stop()
            return result
        }
    }
}
