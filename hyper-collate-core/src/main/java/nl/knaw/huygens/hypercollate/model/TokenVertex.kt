package nl.knaw.huygens.hypercollate.model

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

import eu.interedition.collatex.Token
import java.util.stream.Stream

interface TokenVertex {
    val token: Token?
    fun addIncomingTokenVertex(incoming: TokenVertex)
    val incomingTokenVertexStream: Stream<TokenVertex>
    fun addOutgoingTokenVertex(outgoing: TokenVertex)
    val outgoingTokenVertexStream: Stream<TokenVertex>
    val sigil: String
    val branchPath: List<Int>
}