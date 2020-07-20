package nl.knaw.huygens.hypercollate.tools

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
import nl.knaw.huygens.hypercollate.model.CollationGraph
import nl.knaw.huygens.hypercollate.model.Node
import nl.knaw.huygens.hypercollate.model.TextEdge
import nl.knaw.huygens.hypercollate.model.TextNode
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import kotlin.math.max

class CollationGraphRanking : Iterable<Set<TextNode>>, Function<TextNode, Int> {
    private val _byNode: MutableMap<TextNode, Int> = mutableMapOf()
    private val _byRank: SortedMap<Int, MutableSet<TextNode>> = TreeMap()

//    val byNode: Map<TextNode, Int>
//        get() = _byNode.toMap()
//
//    val byRank: Map<Int, Set<TextNode>>
//        get() = _byRank.toMap()

    val size: Int
        get() = _byRank.keys.size

//    val comparator: Comparator<TextNode>
//        get() = Comparator.comparingInt { key: TextNode -> _byNode[key]!! }

    override fun iterator(): MutableIterator<Set<TextNode>> = _byRank.values.iterator()

    override fun apply(node: TextNode): Int = _byNode[node]!!

    companion object {
        fun of0(graph: CollationGraph): CollationGraphRanking {
            val ranking = CollationGraphRanking()
            for (textNode in graph.traverseTextNodes()) {
                var rank: Int = -1
                for (incomingTextEdge in graph.getIncomingTextEdgeStream(textNode)) {
                    val incomingTextNode = graph.getSource(incomingTextEdge)
                    rank = max(rank, ranking._byNode[incomingTextNode] ?: -1)
                }
                rank += 1
                ranking._byNode[textNode] = rank
                ranking._byRank.computeIfAbsent(rank) { HashSet() }.add(textNode)
            }
            return ranking
        }

        fun of(graph: CollationGraph): CollationGraphRanking {
            val profile = RankingProfile(graph.traverseTextNodes().size, graph.markupStream.count())
            val ranking = CollationGraphRanking()
            // Set<TextNode> nodesRanked = new HashSet<>();
            val nodesToRank: MutableList<TextNode> = ArrayList()
            nodesToRank.add(graph.textStartNode)
            while (nodesToRank.isNotEmpty()) {
                val node = nodesToRank.removeAt(0)
                val canRank = AtomicBoolean(true)
                val rank = AtomicInteger(-1)
                graph.getIncomingEdges(node)
                        .filterIsInstance<TextEdge>()
                        .map { graph.getSource(it) }
                        .forEach { incomingTextNode: Node ->
                            val currentRank = rank.get()
                            val incomingRank = ranking._byNode[incomingTextNode]
                            if (incomingRank == null) {
                                // node has an incoming node that hasn't been ranked yet, so node can't be ranked
                                // yet either.
                                profile.incomingRankIsNull += 1
                                canRank.set(false)
                            } else {
                                val max = max(currentRank, incomingRank)
                                rank.set(max)
                            }
                            profile.incomingTextNodesProcessed += 1
                        }
                graph
                        .getOutgoingTextEdgeStream(node)
                        .map(graph::getTarget)
                        .map(TextNode::class.java::cast)
                        .forEach { e: TextNode -> nodesToRank.add(e); profile.outgoingTextNodesProcessed += 1 }
                if (canRank.get()) {
                    rank.getAndIncrement()
                    ranking._byNode[node] = rank.get()
                    ranking._byRank.computeIfAbsent(rank.get()) { HashSet() }.add(node)
                } else {
                    nodesToRank.add(node)
                }
                profile.whileLoops += 1
            }
            profile.log()
            return ranking
        }
    }

    class RankingProfile(private val textNodes: Int, private val markupNodes: Long) {
        internal var outgoingTextNodesProcessed: Int = 0
        internal var incomingTextNodesProcessed: Int = 0
        internal var whileLoops = 0
        internal var incomingRankIsNull = 0
        private val stopwatch: Stopwatch = Stopwatch.createStarted()

        override fun toString(): String = """
            ranking took ${stopwatch.elapsed(TimeUnit.MILLISECONDS)} ms
            textNodes: $textNodes
            markupNodes: $markupNodes
            whileLoops: $whileLoops
            incomingRankIsNull: $incomingRankIsNull
            incomingTextNodesProcessed: $incomingTextNodesProcessed
            outgoingTextNodesProcessed: $outgoingTextNodesProcessed
            """.trimIndent()

        fun log() {
            stopwatch.stop()
            println(this)
        }
    }
}
