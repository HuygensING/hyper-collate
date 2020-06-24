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

import nl.knaw.huygens.hypercollate.model.SimpleTokenVertex
import nl.knaw.huygens.hypercollate.model.TextNode
import nl.knaw.huygens.hypercollate.model.TokenVertex
import java.util.*
import java.util.stream.Collectors

class CollatedMatch(val collatedNode: TextNode, val witnessVertex: TokenVertex) {
    var nodeRank = 0
    var vertexRank = 0
        private set

    var sigils: Set<String>
    private val branchPaths: MutableMap<String, List<Int>> = HashMap()

    fun hasWitness(sigil: String): Boolean =
            sigils.contains(sigil)

    fun setVertexRank(vertexRank: Int): CollatedMatch {
        this.vertexRank = vertexRank
        return this
    }

    fun getBranchPath(s: String): List<Int>? =
            branchPaths[s]

    override fun toString(): String {
        val sigils = if (collatedNode is TextNode) collatedNode.sigils else sigils
        val sigilString = sigils.stream().sorted().collect(Collectors.joining(","))
        val stringBuilder = StringBuilder("<[").append(sigilString).append("]").append(nodeRank)
        val vString = StringBuilder()
        if (witnessVertex is SimpleTokenVertex) {
            val sv = witnessVertex
            vString.append(sv.sigil).append(sv.indexNumber)
        } else {
            vString.append(witnessVertex.sigil).append(witnessVertex.javaClass.simpleName)
        }
        return stringBuilder.append(",").append(vString.toString()).append(">").toString()
    }

    override fun hashCode(): Int =
            collatedNode.hashCode() * witnessVertex.hashCode()

    override fun equals(other: Any?): Boolean {
        return if (other is CollatedMatch) {
            collatedNode == other.collatedNode && witnessVertex == other.witnessVertex
        } else false
    }

    init {
        val tmp: MutableList<String> = mutableListOf(witnessVertex.sigil)
        branchPaths[witnessVertex.sigil] = witnessVertex.branchPath
        tmp.addAll(collatedNode.sigils)
        sigils = tmp.toSet()
        for (s in collatedNode.sigils) {
            branchPaths[s] = collatedNode.getBranchPath(s)
        }
    }
}
