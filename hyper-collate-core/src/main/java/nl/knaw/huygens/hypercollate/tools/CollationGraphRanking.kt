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

import nl.knaw.huygens.hypercollate.model.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import kotlin.math.max

class CollationGraphRanking : Iterable<Set<TextNode>>, Function<TextNode, Int> {
    private val _byNode: MutableMap<TextNode, Int> = HashMap()
    private val _byRank: SortedMap<Int, MutableSet<TextNode>> = TreeMap()

    val byNode: Map<TextNode, Int>
        get() = _byNode.toMap()

    val byRank: Map<Int, Set<TextNode>>
        get() = _byRank.toMap()

    val size: Int
        get() = _byRank.keys.size

    val comparator: Comparator<TextNode>
        get() = Comparator.comparingInt { key: TextNode -> _byNode[key]!! }

    override fun iterator(): MutableIterator<Set<TextNode>> = _byRank.values.iterator()

    override fun apply(node: TextNode): Int = _byNode[node]!!

    companion object {
        fun of(graph: CollationGraph): CollationGraphRanking {
            val ranking = CollationGraphRanking()
            // Set<TextNode> nodesRanked = new HashSet<>();
            val nodesToRank: MutableList<TextNode> = ArrayList()
            nodesToRank.add(graph.textStartNode)
            while (nodesToRank.isNotEmpty()) {
                val node = nodesToRank.removeAt(0)
                val canRank = AtomicBoolean(true)
                val rank = AtomicInteger(-1)
                graph.getIncomingEdges(node).stream()
                        .filter { obj: Edge? -> TextEdge::class.java.isInstance(obj) }
                        .map { e: Edge -> graph.getSource(e) }
                        .forEach { incomingTextNode: Node? ->
                            val currentRank = rank.get()
                            val incomingRank = ranking._byNode[incomingTextNode]
                            if (incomingRank == null) {
                                // node has an incoming node that hasn't been ranked yet, so node can't be ranked
                                // yet either.
                                canRank.set(false)
                            } else {
                                val max = max(currentRank, incomingRank)
                                rank.set(max)
                            }
                        }
                graph
                        .getOutgoingTextEdgeStream(node)
                        .map { edge: TextEdge? -> graph.getTarget(edge) }
                        .map { obj: TextNode? -> TextNode::class.java.cast(obj) }
                        .forEach { e: TextNode -> nodesToRank.add(e) }
                if (canRank.get()) {
                    rank.getAndIncrement()
                    ranking._byNode[node] = rank.get()
                    ranking._byRank.computeIfAbsent(rank.get()) { r: Int? -> HashSet() }.add(node)
                } else {
                    nodesToRank.add(node)
                }
            }
            return ranking
        }
    }
}