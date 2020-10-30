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

import nl.knaw.huygens.hypercollate.model.TextNode
import kotlin.math.min

data class QuantumCollatedMatchList(val chosenMatches: List<CollatedMatch>, val potentialMatches: List<CollatedMatch>) {

    val isDetermined: Boolean
        get() = potentialMatches.isEmpty()

    val totalSize: Int
        get() = chosenMatches.size + potentialMatches.size

    val fingerprint by lazy {
        val chosen = chosenMatches.joinToString(" ") { it.content }
        val potential = potentialMatches.joinToString(" ") { it.content }
        "$chosen|$potential"
    }

    fun chooseMatch(match: CollatedMatch): QuantumCollatedMatchList {
        val newChosen = chosenMatches + match
        val newPotential = potentialMatches.newPotentialWhenChoosing(match)
        return QuantumCollatedMatchList(newChosen, newPotential)
    }

    fun discardMatch(match: CollatedMatch): QuantumCollatedMatchList {
        val newChosen = chosenMatches
        val newPotential = potentialMatches - match
        return QuantumCollatedMatchList(newChosen, newPotential)
    }

    private fun List<CollatedMatch>.newPotentialWhenChoosing(
            match: CollatedMatch
    ): List<CollatedMatch> =
            this - matchesInvalidatedByChoosing(match)

    private val stringSerialization: String by lazy { "($chosenMatches | $potentialMatches)" }
    override fun toString(): String = stringSerialization

    override fun hashCode(): Int = super.hashCode()

    override fun equals(other: Any?): Boolean =
            other === this

    companion object {
        private fun List<CollatedMatch>.matchesInvalidatedByChoosing(match: CollatedMatch): List<CollatedMatch> {
            val node = match.collatedNode
            val tokenVertexForWitness = match.witnessVertex
            val minNodeRank = match.nodeRank
            val minVertexRank = match.vertexRank
            return filter { m: CollatedMatch ->
                m.collatedNode == node ||
                        m.witnessVertex == tokenVertexForWitness ||
                        m.vertexRank < minVertexRank ||
                        (m.nodeRank < minNodeRank && m hasSigilOverlapWith node)
            }
        }

        // m and node have witnesses in common
        // for those witnesses they have in common, the branch path of one is the start subpath of the
        // other.
        private infix fun CollatedMatch.hasSigilOverlapWith(node: TextNode): Boolean =
                sigils.asSequence()
                        .filter { it in node.sigils }
                        .any { branchPathsOverlap(getBranchPath(it)!!, node.getBranchPath(it)) }

        fun branchPathsOverlap(matchBranchPath: List<Int>, nodeBranchPath: List<Int>): Boolean {
            val minSize = min(matchBranchPath.size, nodeBranchPath.size)
            for (i in 0 until minSize) {
                if (matchBranchPath[i] != nodeBranchPath[i]) {
                    return false
                }
            }
            return true
        }
    }
}
