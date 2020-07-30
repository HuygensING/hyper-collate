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

import nl.knaw.huygens.hypercollate.model.MarkedUpToken
import nl.knaw.huygens.hypercollate.model.SimpleTokenVertex
import nl.knaw.huygens.hypercollate.model.TextNode
import nl.knaw.huygens.hypercollate.model.TokenVertex
import java.util.*

data class CollatedMatch(val collatedNode: TextNode, val witnessVertex: TokenVertex) {
    var nodeRank = 0
    var vertexRank = 0
        private set

    val sigils: Set<String>

    private val branchPaths: MutableMap<String, List<Int>> = HashMap()

    val content: String by lazy {
        if (collatedNode.sigils.isEmpty()) {
            ""
        } else {
            val sigil = collatedNode.sigils.iterator().next()
            val token = collatedNode.getTokenForWitness(sigil) as MarkedUpToken
            token.normalizedContent
        }
    }

    fun hasWitness(sigil: String): Boolean = sigils.contains(sigil)

    fun withVertexRank(vertexRank: Int): CollatedMatch {
        this.vertexRank = vertexRank
        return this
    }

    fun getBranchPath(s: String): List<Int>? = branchPaths[s]

    private val stringSerialization: String by lazy {
        val sigils = collatedNode.sigils
        val sigilString = sigils.sorted().joinToString(",")
        val stringBuilder = StringBuilder("<[").append(sigilString).append("]").append(nodeRank)
        val vString = StringBuilder()
        if (witnessVertex is SimpleTokenVertex) {
            val sv = witnessVertex
            vString.append(sv.sigil).append(sv.indexNumber)
        } else {
            vString.append(witnessVertex.sigil).append(witnessVertex.javaClass.simpleName)
        }
        stringBuilder.append(",").append(vString.toString()).append(">").toString()
    }

    override fun toString(): String = stringSerialization

    private val hashCode: Int by lazy { collatedNode.hashCode() * witnessVertex.hashCode() }

    override fun hashCode(): Int =
            hashCode

    override fun equals(other: Any?): Boolean =
            other === this

    init {
        val tmp: MutableList<String> = mutableListOf(witnessVertex.sigil)
        tmp.addAll(collatedNode.sigils)
        sigils = tmp.toSet()
        for (s in collatedNode.sigils) {
            branchPaths[s] = collatedNode.getBranchPath(s)
        }
        branchPaths[witnessVertex.sigil] = witnessVertex.branchPath
    }
}
