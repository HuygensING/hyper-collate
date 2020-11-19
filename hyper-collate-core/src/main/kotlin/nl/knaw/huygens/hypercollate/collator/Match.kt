package nl.knaw.huygens.hypercollate.collator

/*-
 * #%L
 * hyper-collate-core
 * =======
 * Copyright (C) 2017 - 2020 Huygens ING (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * #L%
 */

import com.google.common.base.Joiner
import nl.knaw.huygens.hypercollate.model.SimpleTokenVertex
import nl.knaw.huygens.hypercollate.model.TokenVertex
import java.util.*

class Match(vararg matchingTokenVertices: TokenVertex) {
    private val tokenVertexMap: MutableMap<String, TokenVertex> = TreeMap()
    private val rankingMap: MutableMap<String, Int> = TreeMap()

    val tokenVertexList: Iterable<TokenVertex>
        get() = tokenVertexMap.values

    fun getTokenVertexForWitness(sigil: String): TokenVertex? =
            tokenVertexMap[sigil]

    fun setRank(sigil: String, rank: Int): Match {
        rankingMap[sigil] = rank
        return this
    }

    fun getRankForWitness(sigil: String): Int? =
            rankingMap[sigil]

    fun getLowestRankForWitnessesOtherThan(s: String): Int =
            rankingMap.entries
                    .filter { it.key != s }
                    .map { it.value }
                    .min()!!
//                    .minOrNull() ?: error("no minimum found") // 2020-09-04: results in java.lang.NoSuchMethodError: 'java.lang.Comparable kotlin.collections.CollectionsKt.minOrNull(java.lang.Iterable)'

    override fun toString(): String {
        val stringBuilder = StringBuilder("<")
        val vertexStrings: MutableList<String> = ArrayList()
        tokenVertexMap.forEach { (sigil: String, vertex: TokenVertex) ->
            val vString = StringBuilder()
            if (vertex is SimpleTokenVertex) {
                vString.append(sigil).append(vertex.indexNumber)
                // vString.append(sigil)
                // .append("[")
                // .append(sv.getIndexNumber())
                // .append(",r")
                // .append(rankingMap.get(sigil))
                // .append("]:'")
                // .append(sv.getContent().replace("\n", "\\n"))
                // .append("'");
            } else {
                vString.append(sigil).append(":").append(vertex.javaClass.simpleName)
            }
            vertexStrings += vString.toString()
        }
        return stringBuilder.append(Joiner.on(",").join(vertexStrings)).append(">").toString()
    }

    val witnessSigils: List<String>
        get() = ArrayList(tokenVertexMap.keys)

    fun addTokenVertex(tokenVertex: SimpleTokenVertex): Match {
        tokenVertexMap[tokenVertex.sigil] = tokenVertex
        return this
    }

    fun hasWitness(sigil: String): Boolean =
            tokenVertexMap.containsKey(sigil)

    init {
        for (mtv in matchingTokenVertices) {
            val sigil = mtv.sigil
            tokenVertexMap[sigil] = mtv
        }
    }
}
