package nl.knaw.huygens.hypercollate.collator

/*-
 * #%L
 * hyper-collate-core
 * =======
 * Copyright (C) 2017 - 2021 Huygens ING (KNAW)
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

import nl.knaw.huygens.hypercollate.model.TokenVertex
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import kotlin.math.max

class VariantWitnessGraphRanking : Iterable<Set<TokenVertex>>, Function<TokenVertex, Int> {
    private val _byVertex: MutableMap<TokenVertex, Int> = HashMap()
    private val _byRank: SortedMap<Int, MutableSet<TokenVertex>> = TreeMap()

//    val byVertex: Map<TokenVertex, Int>
//        get() = _byVertex.toMap()

    val byRank: Map<Int, Set<TokenVertex>>
        get() = _byRank.toMap()

    val size: Int
        get() = _byRank.keys.size

    override fun iterator(): MutableIterator<Set<TokenVertex>> =
            _byRank.values.iterator()

    override fun apply(vertex: TokenVertex): Int =
            _byVertex[vertex]!!

//    val comparator: Comparator<TokenVertex>
//        get() = Comparator.comparingInt(ToIntFunction { key: TokenVertex -> _byVertex.get(key) })

    companion object {
        // private final VariantWitnessGraph graph;
        // VariantWitnessGraphRanking(VariantWitnessGraph graph) {
        // this.graph = graph;
        // }
        fun of(graph: VariantWitnessGraph): VariantWitnessGraphRanking {
            val ranking = VariantWitnessGraphRanking()
            for (v in graph.vertices()) {
                val rank = AtomicInteger(-1)
                v.incomingTokenVertexStream
                        .forEach { incoming: TokenVertex -> rank.set(max(rank.get(), ranking._byVertex[incoming]!!)) }
                rank.getAndIncrement()
                ranking._byVertex[v] = rank.get()
                ranking._byRank.computeIfAbsent(rank.get()) { HashSet() }.add(v)
            }
            return ranking
        }
    }
}
