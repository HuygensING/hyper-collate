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

import eu.interedition.collatex.Token
import java.util.*
import java.util.stream.Stream

class EndTokenVertex internal constructor(override val sigil: String) : TokenVertex {
    private val incomingTokenVertices: MutableList<TokenVertex> = ArrayList()

    override val token: Token?
        get() = null

    override val incomingTokenVertexStream: Stream<TokenVertex>
        get() = incomingTokenVertices.stream()

    override val outgoingTokenVertexStream: Stream<TokenVertex>
        get() = Stream.empty()

    override var branchPath: List<Int>
        get() = ArrayList()
        set(branchPath) {}

    override fun addIncomingTokenVertex(incoming: TokenVertex) {
        incomingTokenVertices.add(incoming)
    }

    override fun addOutgoingTokenVertex(outgoing: TokenVertex) {
        throw RuntimeException(this.javaClass.name + " has no outgoing TokenVertex")
    }
}
