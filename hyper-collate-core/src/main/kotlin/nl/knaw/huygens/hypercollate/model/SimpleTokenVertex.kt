package nl.knaw.huygens.hypercollate.model

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

import java.util.*
import java.util.stream.Stream

class SimpleTokenVertex(override val token: MarkedUpToken) : TokenVertex, Comparable<SimpleTokenVertex> {
    private var _branchPath: List<Int> = emptyList()

    private val incomingVertices: MutableList<TokenVertex> = ArrayList()
    private val outgoingVertices: MutableList<TokenVertex> = ArrayList()

    val content: String
        get() = token.content!!

    val normalizedContent: String
        get() = token.normalizedContent!!

    val parentXPath: String
        get() = token.parentXPath

    val indexNumber: Long
        get() = token.indexNumber

    override val sigil: String
        get() = token.witness.sigil

    override fun addIncomingTokenVertex(incoming: TokenVertex) {
        incomingVertices.add(incoming)
    }

    override val incomingTokenVertexStream: Stream<TokenVertex>
        get() = incomingVertices.stream()

    override fun addOutgoingTokenVertex(outgoing: TokenVertex) {
        outgoingVertices.add(outgoing)
    }

    override val outgoingTokenVertexStream: Stream<TokenVertex>
        get() = outgoingVertices.stream()

    override var branchPath: List<Int>
        get() = _branchPath
        set(value) {
            _branchPath = value
        }

    fun withBranchPath(branchPath: List<Int>): SimpleTokenVertex {
        this.branchPath = branchPath
        return this
    }

    override fun compareTo(other: SimpleTokenVertex): Int {
        return token.indexNumber.compareTo(other.token.indexNumber)
    }

}