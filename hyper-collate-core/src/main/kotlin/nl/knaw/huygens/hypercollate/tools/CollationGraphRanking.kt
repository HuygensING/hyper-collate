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

import nl.knaw.huygens.hypercollate.model.CollationGraph
import nl.knaw.huygens.hypercollate.model.TextEdge
import nl.knaw.huygens.hypercollate.model.TextNode
import java.util.*
import java.util.function.Function
import kotlin.math.max

class CollationGraphRanking : Iterable<Set<TextNode>>, Function<TextNode, Int> {
    private val byNode: MutableMap<TextNode, Int> = mutableMapOf()
    private val byRank: SortedMap<Int, MutableSet<TextNode>> = TreeMap()

    val size: Int
        get() = byRank.keys.size

    override fun iterator(): MutableIterator<Set<TextNode>> = byRank.values.iterator()

    override fun apply(node: TextNode): Int = byNode[node]!!

    companion object {
        fun of(graph: CollationGraph): CollationGraphRanking {
            val ranking = CollationGraphRanking()
            for (textNode in topologicallySortedTextNodes(graph)) {
                var rank: Int = -1
                for (incomingTextEdge in graph.getIncomingTextEdgeList(textNode)) {
                    val incomingTextNode = graph.getSource(incomingTextEdge)
                    rank = max(rank, ranking.byNode[incomingTextNode] ?: -1)
                }
                rank += 1
                ranking.byNode[textNode] = rank
                ranking.byRank.computeIfAbsent(rank) { HashSet() }.add(textNode)
            }
            return ranking
        }

        private fun topologicallySortedTextNodes(graph: CollationGraph): List<TextNode> {
            // https://en.wikipedia.org/wiki/Topological_sorting
            // Kahn's algorithm
            val sorted: MutableList<TextNode> = mutableListOf()
            val todo: MutableSet<TextNode> = mutableSetOf(graph.textStartNode)
            val handledEdges: MutableSet<TextEdge> = mutableSetOf()
            while (todo.isNotEmpty()) {
                val textNode = todo.iterator().next()
                todo.remove(textNode)
                sorted += textNode
                for (e in graph.getOutgoingTextEdgeList(textNode)) {
                    if (e !in handledEdges) {
                        handledEdges += e
                        val node = graph.getTarget(e)
                        if (graph.getIncomingTextEdgeList(node).stream().allMatch { handledEdges.contains(it) }) {
                            todo += node
                        }
                    }
                }
            }
            return sorted
        }
    }

}
